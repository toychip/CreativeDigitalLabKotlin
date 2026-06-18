package com.chat.websocket.broadcast

import com.chat.domain.common.IdGenerator
import com.chat.websocket.dto.ChatMessageResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer

@Component
class RedisMessageBroker(
    @Qualifier("distributedObjectMapper") private val objectMapper: ObjectMapper,
    private val container: RedisMessageListenerContainer,
    private val redisTemplate: RedisTemplate<String, String>
) : MessageListener {

    private var _serverId: String = ""
    val serverId: String
        get() = _serverId
    private var localMessageHandler: BiConsumer<String, ChatMessageResponse>? = null

    private val processedMessages: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val subscribedSessions: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @PostConstruct
    fun init() {
        this._serverId = "server-" + IdGenerator.generate()
    }

    fun setLocalMessageHandler(handler: BiConsumer<String, ChatMessageResponse>) {
        this.localMessageHandler = handler
    }

    fun broadcast(sessionId: String, payload: ChatMessageResponse) {
        try {
            val message = DistributedMessage.create(serverId, sessionId, payload)
            val json = objectMapper.writeValueAsString(message)
            redisTemplate.convertAndSend(SESSION_CHANNEL_PREFIX + sessionId, json)
            log.info("Broadcast to sessionId={}", sessionId)
        } catch (e: Exception) {
            // TODO Production 환경에서 트래픽 파악하여 재시도 규칙 설정 (publish 실패 시 재시도/outbox)
            log.error("Failed to broadcast to sessionId={}", sessionId, e)
        }
    }

    fun subscribe(sessionId: String) {
        if (subscribedSessions.add(sessionId)) {
            val topic = ChannelTopic(SESSION_CHANNEL_PREFIX + sessionId)
            container.addMessageListener(this, topic)
            log.info("Subscribed to sessionId={}", sessionId)
        }
    }

    fun unsubscribe(sessionId: String) {
        if (subscribedSessions.remove(sessionId)) {
            val topic = ChannelTopic(SESSION_CHANNEL_PREFIX + sessionId)
            container.removeMessageListener(this, topic)
            log.info("Unsubscribed from sessionId={}", sessionId)
        }
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        try {
            val json = String(message.body, StandardCharsets.UTF_8)
            val envelope = objectMapper.readValue(json, DistributedMessage::class.java)

            if (envelope.excludeServerId == serverId) {
                return
            }

            if (!processedMessages.add(envelope.id)) {
                return
            }

            localMessageHandler?.accept(envelope.sessionId, envelope.payload)

            if (processedMessages.size > MAX_PROCESSED_MESSAGES) {
                evictOldest()
            }
        } catch (e: Exception) {
            // TODO Production 환경에서 트래픽 파악하여 재시도 규칙 설정 (수신 처리 실패 시 재시도)
            log.error("Failed to handle Redis message", e)
        }
    }

    private fun evictOldest() {
        processedMessages.stream()
            .sorted()
            .limit(EVICT_BATCH_SIZE.toLong())
            .forEach { processedMessages.remove(it) }
    }

    companion object {
        private const val MAX_PROCESSED_MESSAGES = 10000
        private const val EVICT_BATCH_SIZE = MAX_PROCESSED_MESSAGES / 4

        // 사용: broadcast() publish, subscribe()/unsubscribe() listener 등록/해제
        private const val SESSION_CHANNEL_PREFIX = "chat.session."

        private val log = LoggerFactory.getLogger(RedisMessageBroker::class.java)
    }
}

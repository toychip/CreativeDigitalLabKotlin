package com.chat.websocket.registry

import com.chat.application.sessionuser.SessionUserRepository
import com.chat.websocket.broadcast.RedisMessageBroker
import com.chat.websocket.dto.ChatMessageResponse
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@Component
class WsConnectionRegistry(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val redisMessageBroker: RedisMessageBroker,
    private val sessionUserRepository: SessionUserRepository
) {

    // Redis Set 키 prefix. Set 단위 = serverId 1개, 원소 = 이 서버가 구독 중인 sessionId 들
    // 이 서버 인스턴스의 구독 목록
    // joinSession() sessionId 추가, 마지막 ws connection 종료 시 일괄 unsubscribe
    private val userWsConnections: MutableMap<String, MutableSet<WebSocketSession>> = ConcurrentHashMap()

    @PostConstruct
    fun initialize() {
        redisMessageBroker.setLocalMessageHandler(this::sendMessageToLocalSession)
    }

    fun addWsConnection(userId: String, wsConnection: WebSocketSession) {
        log.info("Adding ws connection for userId={}", userId)
        userWsConnections.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(wsConnection)
    }

    fun removeWsConnection(userId: String, wsConnection: WebSocketSession) {
        val connections = userWsConnections[userId] ?: return
        connections.remove(wsConnection)

        if (connections.isNotEmpty()) return
        userWsConnections.remove(userId)

        val totalOpenConnections = userWsConnections.values.stream()
            .flatMap { it.stream() }
            .filter { it.isOpen }
            .count()
        if (totalOpenConnections > 0) {
            return
        }

        val serverSessionsKey = SERVER_SESSIONS_KEY_PREFIX + redisMessageBroker.serverId
        val subscribedSessionIds = redisTemplate.opsForSet().members(serverSessionsKey)
        subscribedSessionIds?.forEach { redisMessageBroker.unsubscribe(it) }
        redisTemplate.delete(serverSessionsKey)
    }

    fun joinSession(userId: String, sessionId: String) {
        val serverSessionsKey = SERVER_SESSIONS_KEY_PREFIX + redisMessageBroker.serverId
        val wasAlreadySubscribed =
            redisTemplate.opsForSet().isMember(serverSessionsKey, sessionId) == true
        if (!wasAlreadySubscribed) {
            redisMessageBroker.subscribe(sessionId)
        }
        redisTemplate.opsForSet().add(serverSessionsKey, sessionId)
        log.info("Joined sessionId={} for userId={}", sessionId, userId)
    }

    fun sendMessageToLocalSession(sessionId: String, payload: ChatMessageResponse) {
        val json: String
        try {
            json = objectMapper.writeValueAsString(payload)
        } catch (e: JsonProcessingException) {
            log.error("Failed to serialize payload for sessionId={}", sessionId, e)
            return
        }
        userWsConnections.forEach { (userId, connections) ->
            if (!isActiveMember(sessionId, userId)) {
                return@forEach
            }
            val closedConnections = HashSet<WebSocketSession>()
            connections.forEach { ws ->
                if (!ws.isOpen) {
                    closedConnections.add(ws)
                    return@forEach
                }
                try {
                    ws.sendMessage(TextMessage(json))
                } catch (e: IOException) {
                    log.warn("Failed to send to userId={}", userId, e)
                    closedConnections.add(ws)
                }
            }
            connections.removeAll(closedConnections)
        }
    }

    fun isUserOnlineLocally(userId: String): Boolean {
        val connections = userWsConnections[userId] ?: return false
        connections.removeIf { !it.isOpen }
        if (connections.isEmpty()) {
            userWsConnections.remove(userId)
            return false
        }
        return true
    }

    private fun isActiveMember(sessionId: String, userId: String): Boolean =
        sessionUserRepository.existsBySessionIdAndUserIdAndIsActiveTrue(sessionId, userId)

    companion object {
        private const val SERVER_SESSIONS_KEY_PREFIX = "chat:server:sessions:"

        private val log = LoggerFactory.getLogger(WsConnectionRegistry::class.java)
    }
}

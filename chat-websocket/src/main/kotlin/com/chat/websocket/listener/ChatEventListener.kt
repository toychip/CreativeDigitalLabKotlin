package com.chat.websocket.listener

import com.chat.domain.event.ChatEvent
import com.chat.domain.event.MessageEvent
import com.chat.websocket.broadcast.RedisMessageBroker
import com.chat.websocket.dto.ChatMessageResponse
import com.chat.websocket.registry.WsConnectionRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ChatEventListener(
    private val registry: WsConnectionRegistry,
    private val broker: RedisMessageBroker
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onChatEvent(event: ChatEvent) {
        if (event is MessageEvent) {
            val response = ChatMessageResponse.fromMessage(event)
            registry.sendMessageToLocalSession(event.sessionId, response)
            broker.broadcast(event.sessionId, response)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ChatEventListener::class.java)
    }
}

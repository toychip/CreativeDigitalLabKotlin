package com.chat.websocket.handler

import com.chat.application.presence.PresenceService
import com.chat.application.service.ChatEventService
import com.chat.application.service.command.MessageCommand
import com.chat.application.sessionuser.SessionUserRepository
import com.chat.application.user.UserRepository
import com.chat.domain.exception.CdlException
import com.chat.domain.exception.ExceptionCode
import com.chat.websocket.dto.ErrorMessage
import com.chat.websocket.dto.InboundMessageType
import com.chat.websocket.registry.WsConnectionRegistry
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.io.EOFException

@Component
class ChatWebSocketHandler(
    private val registry: WsConnectionRegistry,
    private val chatEventService: ChatEventService,
    private val sessionUserRepository: SessionUserRepository,
    private val userRepository: UserRepository,
    private val presenceService: PresenceService,
    @Qualifier("distributedObjectMapper") private val objectMapper: ObjectMapper
) : WebSocketHandler {

    override fun afterConnectionEstablished(wsConnection: WebSocketSession) {
        val userId = getUserId(wsConnection) ?: return

        registry.addWsConnection(userId, wsConnection)
        presenceService.heartbeat(userId)
        log.info("Connection established for userId={}", userId)
        try {
            subscribeActiveSessions(userId)
        } catch (e: Exception) {
            log.error("Error while loading active sessions for userId={}", userId, e)
        }
    }

    override fun handleMessage(wsConnection: WebSocketSession, message: WebSocketMessage<*>) {
        if (message !is TextMessage) {
            log.warn("Unsupported message type: {}", message.javaClass.name)
            return
        }
        val userId = getUserId(wsConnection) ?: return

        var clientEventId: String? = null
        try {
            val root = objectMapper.readTree(message.payload)
            val typeStr = root.path("type").asText(null)
            clientEventId = root.path("clientEventId").asText(null)

            val type = InboundMessageType.parseType(typeStr)
            when (type) {
                InboundMessageType.SEND_MESSAGE -> handleSendMessage(userId, root)
                InboundMessageType.EDIT_MESSAGE -> handleEditMessage(userId, root)
                InboundMessageType.DELETE_MESSAGE -> handleDeleteMessage(userId, root)
                InboundMessageType.HEALTHCHECK -> presenceService.heartbeat(userId)
            }
            // HEALTHCHECK 는 휘발성 하트비트 — 매번 DB(last_seen_at) 쓰지 않음(쓰기 증폭 회피). online 은 Redis TTL 로만.
            if (type != InboundMessageType.HEALTHCHECK) {
                touchLastSeen(userId)
            }
        } catch (e: CdlException) {
            log.warn("CdlException: code={}, detail={}", e.code, e.detail)
            sendError(wsConnection, e, clientEventId)
        } catch (e: Exception) {
            log.error("Failed to handle message", e)
            sendError(wsConnection, CdlException(ExceptionCode.INVALID_MESSAGE_FORMAT), clientEventId)
        }
    }

    override fun handleTransportError(wsConnection: WebSocketSession, exception: Throwable) {
        val userId = getUserId(wsConnection)
        if (exception is EOFException) {
            log.debug("WebSocket closed by client. userId={}", userId)
        } else {
            log.error("WebSocket transport error. userId={}", userId, exception)
        }
        if (userId != null) {
            registry.removeWsConnection(userId, wsConnection)
            markOfflineIfNoLocalConnections(userId)
        }
    }

    override fun afterConnectionClosed(wsConnection: WebSocketSession, closeStatus: CloseStatus) {
        val userId = getUserId(wsConnection)
        if (userId != null) {
            registry.removeWsConnection(userId, wsConnection)
            touchLastSeen(userId)
            markOfflineIfNoLocalConnections(userId)
            log.info("Connection removed for userId={}", userId)
        }
    }

    // 이 유저의 로컬 연결이 모두 사라졌으면 즉시 offline. 남은 연결이 있으면(멀티 디바이스) 유지.
    // 비정상 종료로 이 경로가 안 불려도 presence TTL 이 만료되며 결국 offline 으로 수렴한다.
    private fun markOfflineIfNoLocalConnections(userId: String) {
        if (!registry.isUserOnlineLocally(userId)) {
            presenceService.markOffline(userId)
        }
    }

    private fun subscribeActiveSessions(userId: String) {
        try {
            val activeSessionIds = sessionUserRepository.findByUserIdAndIsActiveTrue(userId)
                .map { it.sessionId }
            activeSessionIds.forEach { sessionId -> registry.joinSession(userId, sessionId) }
            log.info("Loaded {} active sessions for userId={}", activeSessionIds.size, userId)
        } catch (e: Exception) {
            log.error("Failed to load active sessions for userId={}", userId, e)
        }
    }

    override fun supportsPartialMessages(): Boolean = false

    private fun handleSendMessage(userId: String, root: JsonNode) {
        val sessionId = requireSessionId(root)
        val clientEventId = requireClientEventId(root)
        val content = root.path("content").asText(null)

        chatEventService.appendMessage(MessageCommand.sent(sessionId, clientEventId, userId, content))
    }

    private fun handleEditMessage(userId: String, root: JsonNode) {
        val sessionId = requireSessionId(root)
        val clientEventId = requireClientEventId(root)
        val messageId = requireMessageId(root)
        val content = root.path("content").asText(null)

        chatEventService.appendMessage(MessageCommand.edited(sessionId, clientEventId, userId, messageId, content))
    }

    private fun handleDeleteMessage(userId: String, root: JsonNode) {
        val sessionId = requireSessionId(root)
        val clientEventId = requireClientEventId(root)
        val messageId = requireMessageId(root)

        chatEventService.appendMessage(MessageCommand.deleted(sessionId, clientEventId, userId, messageId))
    }

    private fun requireSessionId(root: JsonNode): String {
        val sessionId = root.path("sessionId").asText(null)
        if (sessionId == null || sessionId.isBlank()) {
            throw CdlException(ExceptionCode.SESSION_ID_REQUIRED)
        }
        return sessionId
    }

    private fun requireClientEventId(root: JsonNode): String {
        val clientEventId = root.path("clientEventId").asText(null)
        if (clientEventId == null || clientEventId.isBlank()) {
            throw CdlException(ExceptionCode.CLIENT_EVENT_ID_REQUIRED)
        }
        return clientEventId
    }

    private fun requireMessageId(root: JsonNode): String {
        val messageId = root.path("messageId").asText(null)
        if (messageId == null || messageId.isBlank()) {
            throw CdlException(ExceptionCode.MESSAGE_ID_REQUIRED)
        }
        return messageId
    }

    private fun sendError(wsConnection: WebSocketSession, e: CdlException, clientEventId: String?) {
        try {
            val error = ErrorMessage.from(e, clientEventId)
            val json = objectMapper.writeValueAsString(error)
            wsConnection.sendMessage(TextMessage(json))
        } catch (ex: Exception) {
            log.error("Failed to send error message", ex)
        }
    }

    private fun getUserId(wsConnection: WebSocketSession): String? {
        val value = wsConnection.attributes[SessionHandshakeInterceptor.USER_ID_ATTRIBUTE]
        return value as? String
    }

    private fun touchLastSeen(userId: String) {
        try {
            userRepository.findByUserId(userId).ifPresent { user ->
                user.touchLastSeen()
                userRepository.save(user)
            }
        } catch (e: Exception) {
            log.warn("Failed to touch lastSeenAt for userId={}", userId, e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ChatWebSocketHandler::class.java)
    }
}

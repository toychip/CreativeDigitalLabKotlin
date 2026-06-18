package com.chat.websocket.handler

import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

// 인증/인가는 과제 비목표. query param {@code ?userId=} 만으로 식별
@Component
class SessionHandshakeInterceptor : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val userId = resolveUserId(request) ?: return false
        attributes[USER_ID_ATTRIBUTE] = userId
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }

    private fun resolveUserId(request: ServerHttpRequest): String? {
        val query = request.uri.query ?: return null
        val userId = parseQuery(query)["userId"]
        return if (userId.isNullOrBlank()) null else userId
    }

    private fun parseQuery(query: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        query.split("&")
            .map { it.split("=", limit = 2) }
            .filter { it.size == 2 }
            .forEach { parts -> result.putIfAbsent(parts[0], parts[1]) }
        return result
    }

    companion object {
        const val USER_ID_ATTRIBUTE = "userId"
    }
}

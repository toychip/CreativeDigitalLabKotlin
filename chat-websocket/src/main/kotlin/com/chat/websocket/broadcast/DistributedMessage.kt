package com.chat.websocket.broadcast

import com.chat.domain.common.IdGenerator
import com.chat.websocket.dto.ChatMessageResponse
import java.time.Instant

data class DistributedMessage(
    val id: String,
    val serverId: String,
    val sessionId: String,
    val excludeServerId: String,
    val timestamp: Instant,
    val payload: ChatMessageResponse
) {
    companion object {
        fun create(
            serverId: String,
            sessionId: String,
            payload: ChatMessageResponse
        ): DistributedMessage =
            DistributedMessage(
                IdGenerator.generate(),
                serverId,
                sessionId,
                serverId,
                Instant.now(),
                payload
            )
    }
}

package com.chat.websocket.dto

import com.chat.domain.event.MessageEvent
import com.chat.domain.session.MessageStatus
import java.time.Instant

data class ChatMessageResponse(
    val messageStatus: MessageStatus,
    val sessionId: String,
    val seq: Long,
    val createdAt: Instant,
    val clientEventId: String,
    val senderId: String,
    val messageId: String,
    val content: String?
) {
    companion object {
        fun fromMessage(event: MessageEvent): ChatMessageResponse =
            ChatMessageResponse(
                event.type,
                event.sessionId,
                event.seq,
                event.createdAt,
                event.clientEventId,
                event.senderId,
                event.messageId,
                event.content
            )
    }
}

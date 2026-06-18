package com.chat.application.message

import com.chat.domain.session.MessageStatus
import java.time.LocalDateTime

data class MessageView(
    val messageId: String,
    val senderId: String,
    val content: String?,
    val status: MessageStatus,
    val seq: Long,
    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(entity: MessageEntity): MessageView =
            MessageView(
                entity.messageId,
                entity.senderId,
                entity.content,
                entity.status,
                entity.seq,
                entity.createdAt
            )
    }
}

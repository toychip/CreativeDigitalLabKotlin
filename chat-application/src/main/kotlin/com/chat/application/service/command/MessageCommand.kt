package com.chat.application.service.command

import com.chat.domain.common.IdGenerator
import com.chat.domain.session.MessageStatus

data class MessageCommand(
    val sessionId: String,
    val clientEventId: String,
    val senderId: String,
    val messageId: String,
    val content: String?,
    val type: MessageStatus
) {
    companion object {
        fun sent(
            sessionId: String,
            clientEventId: String,
            senderId: String,
            content: String?
        ): MessageCommand =
            MessageCommand(
                sessionId,
                clientEventId,
                senderId,
                IdGenerator.generate(),
                content,
                MessageStatus.SENT
            )

        fun edited(
            sessionId: String,
            clientEventId: String,
            senderId: String,
            messageId: String,
            content: String?
        ): MessageCommand =
            MessageCommand(
                sessionId,
                clientEventId,
                senderId,
                messageId,
                content,
                MessageStatus.EDITED
            )

        fun deleted(
            sessionId: String,
            clientEventId: String,
            senderId: String,
            messageId: String
        ): MessageCommand =
            MessageCommand(
                sessionId,
                clientEventId,
                senderId,
                messageId,
                null,
                MessageStatus.DELETED
            )
    }
}

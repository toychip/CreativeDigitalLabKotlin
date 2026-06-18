package com.chat.application.session

import com.chat.domain.session.ChatSession
import com.chat.domain.session.Message
import com.chat.domain.session.MessageStatus
import com.chat.domain.session.SessionStatus

data class TimelineResponse(
    val sessionId: String?,
    val status: SessionStatus?,
    val participants: Set<String>,
    val messages: List<MessageSummary>
) {
    data class MessageSummary(
        val messageId: String,
        val senderId: String,
        val content: String?,
        val status: MessageStatus
    ) {
        companion object {
            fun from(m: Message): MessageSummary =
                MessageSummary(m.messageId, m.senderId, m.content, m.status)
        }
    }

    companion object {
        fun from(session: ChatSession): TimelineResponse {
            val messages = session.getMessages()
                .map { MessageSummary.from(it) }
            return TimelineResponse(
                session.sessionId,
                session.status,
                session.getParticipants(),
                messages
            )
        }
    }
}

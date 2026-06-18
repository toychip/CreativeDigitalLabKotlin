package com.chat.application.session

import com.chat.domain.session.SessionStatus
import java.time.LocalDateTime

data class SessionView(
    val sessionId: String,
    val status: SessionStatus,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?
) {
    companion object {
        fun from(entity: SessionEntity): SessionView =
            SessionView(
                entity.sessionId,
                entity.status,
                entity.startedAt,
                entity.endedAt
            )
    }
}

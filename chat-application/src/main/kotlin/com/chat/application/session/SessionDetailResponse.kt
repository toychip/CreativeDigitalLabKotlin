package com.chat.application.session

import com.chat.domain.session.SessionStatus
import java.time.LocalDateTime

data class SessionDetailResponse(
    val sessionId: String,
    val status: SessionStatus,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val participants: List<ParticipantView>
) {
    companion object {
        fun of(session: SessionEntity, participants: List<ParticipantView>): SessionDetailResponse =
            SessionDetailResponse(
                session.sessionId,
                session.status,
                session.startedAt,
                session.endedAt,
                participants
            )
    }
}

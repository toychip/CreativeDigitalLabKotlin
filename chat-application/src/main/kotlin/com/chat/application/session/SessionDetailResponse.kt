package com.chat.application.session

import com.chat.application.sessionuser.SessionUserEntity
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
        fun of(session: SessionEntity, members: List<SessionUserEntity>): SessionDetailResponse =
            SessionDetailResponse(
                session.sessionId,
                session.status,
                session.startedAt,
                session.endedAt,
                members.map { ParticipantView.from(it) }
            )
    }
}

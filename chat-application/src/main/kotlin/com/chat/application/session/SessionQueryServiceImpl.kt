package com.chat.application.session

import com.chat.application.event.EventRepository
import com.chat.application.sessionuser.SessionUserRepository
import com.chat.domain.event.ChatEvent
import com.chat.domain.exception.CdlException
import com.chat.domain.exception.ExceptionCode
import com.chat.domain.session.SessionStatus
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime

@Service
class SessionQueryServiceImpl(
    private val sessionRepository: SessionRepository,
    private val sessionUserRepository: SessionUserRepository,
    private val eventRepository: EventRepository
) : SessionQueryService {

    @Transactional(readOnly = true)
    override fun getSessionDetail(sessionId: String): SessionDetailResponse {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { CdlException(ExceptionCode.SESSION_NOT_FOUND) }
        val members = sessionUserRepository.findBySessionIdOrderByJoinedAtAsc(sessionId)
        return SessionDetailResponse.of(session, members)
    }

    @Transactional(readOnly = true)
    override fun getSessions(status: SessionStatus?, from: LocalDateTime?, to: LocalDateTime?, cursor: String?, limit: Int): SessionPageResponse {
        val capped = maxOf(1, minOf(limit, MAX_LIMIT))
        val sessions = sessionRepository.findWithCursor(status, from, to, cursor, Limit.of(capped))
        return SessionPageResponse.of(sessions, capped)
    }

    @Transactional(readOnly = true)
    override fun loadTimelineEvents(sessionId: String, at: Instant?): List<ChatEvent> {
        if (!sessionRepository.existsById(sessionId)) {
            throw CdlException(ExceptionCode.SESSION_NOT_FOUND)
        }
        val entities = if (at != null) {
            eventRepository.findEventsUpTo(sessionId, at)
        } else {
            eventRepository.findAllBySessionId(sessionId)
        }
        return entities.map { it.toDomain() }
    }

    companion object {
        private const val MAX_LIMIT = 100
    }
}

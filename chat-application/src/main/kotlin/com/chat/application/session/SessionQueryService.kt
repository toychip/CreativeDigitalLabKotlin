package com.chat.application.session

import com.chat.domain.session.SessionStatus
import java.time.Instant
import java.time.LocalDateTime

interface SessionQueryService {

    fun getSessionDetail(sessionId: String): SessionDetailResponse

    fun getSessions(status: SessionStatus?, from: LocalDateTime?, to: LocalDateTime?, cursor: String?, limit: Int): SessionPageResponse

    /** 특정 시점(at) 상태 복원 — 이벤트 fold. at 이 null 이면 현재 기준 전체 */
    fun getTimeline(sessionId: String, at: Instant?): TimelineResponse
}

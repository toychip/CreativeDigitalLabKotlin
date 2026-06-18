package com.chat.application.session

import com.chat.domain.event.ChatEvent
import com.chat.domain.session.SessionStatus
import java.time.Instant
import java.time.LocalDateTime

interface SessionQueryService {

    fun getSessionDetail(sessionId: String): SessionDetailResponse

    fun getSessions(status: SessionStatus?, from: LocalDateTime?, to: LocalDateTime?, cursor: String?, limit: Int): SessionPageResponse

    /**
     * 특정 시점(at) 복원을 위한 이벤트 로드. at 이 null 이면 전체.
     * 트랜잭션/커넥션은 이 조회까지만 점유하고, fold(replay)는 TimelineRestoreService 가 트랜잭션 밖에서 수행한다.
     */
    fun loadTimelineEvents(sessionId: String, at: Instant?): List<ChatEvent>
}

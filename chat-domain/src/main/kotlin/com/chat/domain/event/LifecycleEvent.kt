package com.chat.domain.event

import com.chat.domain.common.IdGenerator
import com.chat.domain.session.SessionStatus
import java.time.Instant

/**
 * 세션 상태 전환 이벤트
 * status 가 ACTIVE → 시작 또는 재개
 * status 가 SUSPENDED → 중단
 * status 가 ENDED → 종료
 */
data class LifecycleEvent(
    override val eventId: String,
    override val sessionId: String,
    override val clientEventId: String,
    override val seq: Long,
    override val createdAt: Instant,
    val status: SessionStatus
) : ChatEvent {

    companion object {
        fun create(
            sessionId: String,
            clientEventId: String,
            seq: Long,
            status: SessionStatus
        ): LifecycleEvent =
            LifecycleEvent(
                IdGenerator.generate(),
                sessionId,
                clientEventId,
                seq,
                Instant.now(),
                status
            )
    }
}

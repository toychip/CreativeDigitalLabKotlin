package com.chat.domain.event

import com.chat.domain.common.IdGenerator
import java.time.Instant

/**
 * 사용자 입장/퇴장 이벤트
 */
data class UserEvent(
    override val eventId: String,
    override val sessionId: String,
    override val clientEventId: String,
    override val seq: Long,
    override val createdAt: Instant,
    val userId: String,
    val type: Type
) : ChatEvent {

    enum class Type {
        JOINED,
        LEFT
    }

    companion object {
        /**
         * 신규 이벤트 생성. eventId 와 createdAt 은 도메인이 발급.
         */
        fun create(
            sessionId: String,
            clientEventId: String,
            seq: Long,
            userId: String,
            type: Type
        ): UserEvent =
            UserEvent(
                IdGenerator.generate(),
                sessionId,
                clientEventId,
                seq,
                Instant.now(),
                userId,
                type
            )
    }
}

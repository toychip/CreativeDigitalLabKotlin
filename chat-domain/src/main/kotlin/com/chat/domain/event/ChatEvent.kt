package com.chat.domain.event

import java.time.Instant

/**
 * 채팅에서 발생하는 모든 이벤트의 추상 부모
 */
sealed interface ChatEvent {

    val eventId: String

    val sessionId: String

    val clientEventId: String

    val seq: Long

    val createdAt: Instant
}

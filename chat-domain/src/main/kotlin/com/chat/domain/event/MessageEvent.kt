package com.chat.domain.event

import com.chat.domain.common.IdGenerator
import com.chat.domain.session.MessageStatus
import java.time.Instant

/**
 * 메시지 전송/수정/삭제 이벤트
 */
data class MessageEvent(
    override val eventId: String,
    override val sessionId: String,
    override val clientEventId: String,
    override val seq: Long,
    override val createdAt: Instant,
    val senderId: String,
    val messageId: String,
    val content: String?,
    val type: MessageStatus
) : ChatEvent {

    companion object {
        /**
         * 신규 이벤트 생성. eventId 와 createdAt 은 도메인이 발급.
         * messageId 는 호출부에서 결정 (SENT 면 새 ID, EDITED/DELETED 면 기존 메시지 ID 재사용).
         */
        fun create(
            sessionId: String,
            clientEventId: String,
            seq: Long,
            senderId: String,
            messageId: String,
            content: String?,
            type: MessageStatus
        ): MessageEvent =
            MessageEvent(
                IdGenerator.generate(),
                sessionId,
                clientEventId,
                seq,
                Instant.now(),
                senderId,
                messageId,
                content,
                type
            )
    }
}

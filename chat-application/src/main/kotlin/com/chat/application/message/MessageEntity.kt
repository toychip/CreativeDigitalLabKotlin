package com.chat.application.message

import com.chat.application.common.BaseEntity
import com.chat.domain.session.MessageStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "messages",
    indexes = [
        Index(name = "idx_message_session_seq", columnList = "session_id, seq"),
        Index(name = "idx_message_sender", columnList = "sender_id")
    ]
)
class MessageEntity private constructor(
    @Id
    @Column(name = "message_id", length = 36, nullable = false, updatable = false)
    val messageId: String,

    @Column(name = "session_id", length = 36, nullable = false)
    val sessionId: String,

    @Column(name = "sender_id", length = 36, nullable = false)
    val senderId: String,

    @Column(columnDefinition = "TEXT")
    var content: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MessageStatus,

    @Column(nullable = false)
    val seq: Long
) : BaseEntity() {

    fun edit(content: String?) {
        // DELETED 는 terminal — 순서 역전으로 DELETE 뒤 EDIT 가 와도 부활 금지
        if (this.status == MessageStatus.DELETED) {
            return
        }
        this.content = content
        this.status = MessageStatus.EDITED
    }

    fun delete() {
        this.status = MessageStatus.DELETED
    }

    companion object {
        fun send(
            messageId: String,
            sessionId: String,
            senderId: String,
            content: String?,
            seq: Long
        ): MessageEntity =
            MessageEntity(messageId, sessionId, senderId, content, MessageStatus.SENT, seq)
    }
}

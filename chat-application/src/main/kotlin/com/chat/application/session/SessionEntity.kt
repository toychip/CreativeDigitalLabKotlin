package com.chat.application.session

import com.chat.application.common.BaseEntity
import com.chat.domain.session.SessionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "sessions",
    indexes = [
        Index(name = "idx_sessions_status", columnList = "status"),
        Index(name = "idx_sessions_started_at", columnList = "started_at")
    ]
)
class SessionEntity private constructor(
    @Id
    @Column(name = "session_id", length = 36, nullable = false, updatable = false)
    val sessionId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SessionStatus,

    @Column(nullable = false)
    var startedAt: LocalDateTime,

    @Column
    var endedAt: LocalDateTime?
) : BaseEntity() {

    fun changeStatus(status: SessionStatus) {
        // ENDED 는 terminal — 순서 역전/중복 이벤트로도 되살아나지 않음
        if (this.status == SessionStatus.ENDED) {
            return
        }
        this.status = status
        if (status == SessionStatus.ENDED && this.endedAt == null) {
            this.endedAt = LocalDateTime.now()
        }
    }

    companion object {
        fun start(sessionId: String): SessionEntity =
            SessionEntity(
                sessionId,
                SessionStatus.ACTIVE,
                LocalDateTime.now(),
                null
            )
    }
}

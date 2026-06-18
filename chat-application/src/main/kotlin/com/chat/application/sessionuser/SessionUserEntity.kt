package com.chat.application.sessionuser

import com.chat.application.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "session_users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_session_user", columnNames = ["session_id", "user_id"])
    ],
    indexes = [
        Index(name = "idx_session_user_session_id", columnList = "session_id"),
        Index(name = "idx_session_user_user_id", columnList = "user_id"),
        Index(name = "idx_session_user_active", columnList = "is_active")
    ]
)
class SessionUserEntity private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @Column(name = "session_id", length = 36, nullable = false)
    val sessionId: String,

    @Column(name = "user_id", length = 36, nullable = false)
    val userId: String,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val role: MemberRole,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @Column(nullable = false)
    var joinedAt: LocalDateTime,

    @Column
    var leftAt: LocalDateTime?
) : BaseEntity() {

    /** leave 후 재참여: 기존 row 재활성화 */
    fun rejoin() {
        this.isActive = true
        this.leftAt = null
        this.joinedAt = LocalDateTime.now()
    }

    companion object {
        fun join(sessionId: String, userId: String, role: MemberRole): SessionUserEntity =
            SessionUserEntity(
                null,
                sessionId,
                userId,
                role,
                true,
                LocalDateTime.now(),
                null
            )
    }
}

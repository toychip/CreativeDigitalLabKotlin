package com.chat.application.user

import com.chat.application.common.BaseEntity
import com.chat.domain.common.IdGenerator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_user_id", columnNames = ["user_id"])
    ]
)
class UserEntity private constructor(
    // 서버 발급 PK (내부 식별자)
    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    val id: String,

    // 클라이언트가 지정하는 식별자 (로그인 아이디). WebSocket ?userId=, senderId 등에서 사용
    @Column(name = "user_id", nullable = false, length = 50, updatable = false)
    val userId: String,

    @Column(nullable = false, length = 50)
    val username: String,

    @Column(length = 50)
    var status: String?,

    @Column
    var lastSeenAt: LocalDateTime?
) : BaseEntity() {

    fun touchLastSeen() {
        this.lastSeenAt = LocalDateTime.now()
    }

    companion object {
        fun create(userId: String, username: String): UserEntity =
            UserEntity(
                IdGenerator.generate(),
                userId,
                username,
                null,
                null
            )
    }
}

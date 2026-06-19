package com.chat.application.sessionuser

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SessionUserRepository : JpaRepository<SessionUserEntity, Long> {

    /** 참여 이력 전체 (active + 퇴장), 참여 순 */
    fun findBySessionIdOrderByJoinedAtAsc(sessionId: String): List<SessionUserEntity>

    fun findByUserIdAndIsActiveTrue(userId: String): List<SessionUserEntity>

    /** isActive 무관하게 조회 — rejoin 시 기존 row 찾기용 */
    fun findBySessionIdAndUserId(sessionId: String, userId: String): Optional<SessionUserEntity>

    fun existsBySessionIdAndUserIdAndIsActiveTrue(sessionId: String, userId: String): Boolean

    @Modifying
    @Query(
        """
        UPDATE SessionUserEntity su
        SET su.isActive = false, su.leftAt = CURRENT_TIMESTAMP
        WHERE su.sessionId = :sessionId AND su.userId = :userId AND su.isActive = true
        """
    )
    fun leaveSession(sessionId: String, userId: String)
}

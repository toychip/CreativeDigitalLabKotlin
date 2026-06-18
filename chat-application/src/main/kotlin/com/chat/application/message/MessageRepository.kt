package com.chat.application.message

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : JpaRepository<MessageEntity, String> {

    fun findBySessionIdOrderBySeqDesc(sessionId: String, limit: Limit): List<MessageEntity>

    /** BEFORE — 커서보다 과거 (seq < cursor, 내림차순) */
    fun findBySessionIdAndSeqLessThanOrderBySeqDesc(sessionId: String, cursor: Long, limit: Limit): List<MessageEntity>

    /** AFTER — 커서보다 최신 (seq > cursor, 오름차순; 호출부에서 reverse) */
    fun findBySessionIdAndSeqGreaterThanOrderBySeqAsc(sessionId: String, cursor: Long, limit: Limit): List<MessageEntity>
}

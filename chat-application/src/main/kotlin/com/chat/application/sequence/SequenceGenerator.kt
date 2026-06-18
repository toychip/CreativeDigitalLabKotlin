package com.chat.application.sequence

import com.chat.application.event.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

// 세션별 시퀀스 발급: Redis INCR, 실패 시 DB MAX(seq)+1 폴백.
//
// 시나리오:
//  1) 정상              → Redis INCR 결과 반환
//  2) Redis 연결 실패    → DataAccessException catch → DB MAX(seq)+1
//  3) Redis 재기동 + 데이터 유실 → AOF + RDB snapshot 으로 카운터 복원
@Component
class SequenceGenerator(
    private val redisTemplate: RedisTemplate<String, String>,
    private val eventRepository: EventRepository
) {

    fun nextSeq(sessionId: String): Long {
        val key = KEY_PREFIX + sessionId
        try {
            val result = redisTemplate.opsForValue().increment(key)
            if (result != null) {
                return result
            }
        } catch (e: DataAccessException) {
            log.warn("Redis INCR failed for sessionId={}, fallback to DB MAX", sessionId, e)
        }
        // TODO Production: DB 폴백은 비원자적 — 동시 요청 시 seq 충돌(uk_events_session_seq) 가능. 분산 락 또는 DB 시퀀스로 대체 고려
        return eventRepository.findMaxSeqBySessionId(sessionId).orElse(0L) + 1
    }

    companion object {
        private const val KEY_PREFIX = "chat:seq:"
        private val log = LoggerFactory.getLogger(SequenceGenerator::class.java)
    }
}

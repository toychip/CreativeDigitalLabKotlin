package com.chat.application.event

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface EventRepository : JpaRepository<EventEntity, String> {

    fun existsBySessionId(sessionId: String): Boolean

    /**
     * 시점 복원 경계: created_at <= at 인 이벤트 중 가장 큰 seq.
     * created_at 은 서버별 시계(클럭 스큐·브로드캐스트 역전)라 신뢰 불가 → 이 seq 를 경계로 잡아
     * seq 연속 prefix 를 복원한다. {@code idx_events_session_created} 범위 스캔.
     * at 이전 이벤트가 없으면 Optional.empty().
     */
    @Query(
        """
        SELECT MAX(e.seq) FROM EventEntity e
        WHERE e.sessionId = :sessionId AND e.createdAt <= :at
        """
    )
    fun findMaxSeqUpTo(
        @Param("sessionId") sessionId: String,
        @Param("at") at: Instant
    ): Optional<Long>

    /**
     * 시점 복원: seq <= :maxSeq 인 이벤트들 seq 순(연속 prefix) 반환.
     * {@code uk_events_session_seq} 인덱스 활용. 시계가 아닌 원자적 seq 기준이라 결정적.
     */
    @Query(
        """
        SELECT e FROM EventEntity e
        WHERE e.sessionId = :sessionId AND e.seq <= :maxSeq
        ORDER BY e.seq
        """
    )
    fun findEventsUpToSeq(
        @Param("sessionId") sessionId: String,
        @Param("maxSeq") maxSeq: Long
    ): List<EventEntity>

    /**
     * 타임라인 전체 조회: at 파라미터 없이 세션의 모든 이벤트 seq 순 반환
     */
    @Query(
        """
        SELECT e FROM EventEntity e
        WHERE e.sessionId = :sessionId
        ORDER BY e.seq
        """
    )
    fun findAllBySessionId(@Param("sessionId") sessionId: String): List<EventEntity>

    /**
     * SequenceGenerator 의 폴백 초기화용
     * Redis 카운터 유실 시 DB 의 가장 큰 seq 를 조회해서 그 위부터 다시 발급한다.
     * 이벤트가 없는 세션은 Optional.empty() — 호출부에서 0L 로 대체
     */
    @Query(
        """
        SELECT MAX(e.seq) FROM EventEntity e
        WHERE e.sessionId = :sessionId
        """
    )
    fun findMaxSeqBySessionId(@Param("sessionId") sessionId: String): Optional<Long>
}

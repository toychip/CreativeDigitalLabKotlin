package com.chat.application.session

import com.chat.domain.event.ChatEvent

interface TimelineRestoreService {

    /**
     * 이벤트 fold(replay) 로 시점 상태 복원.
     * DB 조회(SessionQueryService.loadTimelineEvents)와 분리된 순수 인메모리 작업으로,
     * 트랜잭션/커넥션 밖에서 수행한다.
     */
    fun restore(events: List<ChatEvent>): TimelineResponse
}

package com.chat.application.session

import com.chat.domain.event.ChatEvent
import com.chat.domain.session.ChatSession
import org.springframework.stereotype.Service

@Service
class TimelineRestoreServiceImpl : TimelineRestoreService {

    // @Transactional 없음 — DB 커넥션을 잡지 않는 순수 CPU fold.
    // 이벤트가 많아 fold 가 오래 걸려도 트랜잭션/커넥션을 점유하지 않는다.
    override fun restore(events: List<ChatEvent>): TimelineResponse {
        val session = ChatSession.loadFromEvents(events)
        return TimelineResponse.from(session)
    }
}

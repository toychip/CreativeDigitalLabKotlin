package com.chat.application.listener

import com.chat.application.config.AsyncConfig
import com.chat.application.service.ProjectionService
import com.chat.domain.event.ChatEvent
import com.chat.domain.event.LifecycleEvent
import com.chat.domain.event.MessageEvent
import com.chat.domain.event.UserEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProjectionUpdater(
    private val projectionService: ProjectionService
) {

    @Async(AsyncConfig.PROJECTION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ChatEvent) {
        try {
            when (event) {
                is LifecycleEvent -> projectionService.handleLifecycle(event)
                is UserEvent -> projectionService.handleUser(event)
                is MessageEvent -> projectionService.handleMessage(event)
            }
        } catch (ex: Exception) {
            // TODO Production 환경에서 트래픽 파악하여 재시도 규칙 설정 (projection 실패 시 재시도/DLQ)
            log.error("Projection update failed. eventId={}, sessionId={}", event.eventId, event.sessionId, ex)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProjectionUpdater::class.java)
    }
}

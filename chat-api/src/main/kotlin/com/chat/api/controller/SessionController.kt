package com.chat.api.controller

import com.chat.api.controller.docs.SessionControllerDocs
import com.chat.api.dto.ClientEventRequest
import com.chat.api.dto.SessionCreateRequest
import com.chat.api.dto.SessionCreateResponse
import com.chat.api.dto.UserSessionRequest
import com.chat.application.service.ChatEventService
import com.chat.application.service.command.LifecycleCommand
import com.chat.application.service.command.UserCommand
import com.chat.application.session.SessionDetailResponse
import com.chat.application.session.SessionPageResponse
import com.chat.application.session.SessionQueryService
import com.chat.application.session.TimelineResponse
import com.chat.application.session.TimelineRestoreService
import com.chat.domain.common.IdGenerator
import com.chat.domain.event.UserEvent
import com.chat.domain.session.SessionStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDateTime

@RestController
@RequestMapping("/sessions")
class SessionController(
    private val chatEventService: ChatEventService,
    private val sessionQueryService: SessionQueryService,
    private val timelineRestoreService: TimelineRestoreService
) : SessionControllerDocs {

    @PostMapping
    override fun createSession(@RequestBody request: SessionCreateRequest): SessionCreateResponse {
        val sessionId = IdGenerator.generate()
        chatEventService.createSession(sessionId, request.clientEventId, request.creatorUserId)
        return SessionCreateResponse(sessionId)
    }

    @PostMapping("/{sessionId}/join")
    override fun joinSession(
        @PathVariable sessionId: String,
        @RequestBody request: UserSessionRequest
    ) {
        chatEventService.appendUser(
            UserCommand(sessionId, request.clientEventId, request.userId, UserEvent.Type.JOINED))
    }

    @PostMapping("/{sessionId}/leave")
    override fun leaveSession(
        @PathVariable sessionId: String,
        @RequestBody request: UserSessionRequest
    ) {
        chatEventService.appendUser(
            UserCommand(sessionId, request.clientEventId, request.userId, UserEvent.Type.LEFT))
    }

    @PostMapping("/{sessionId}/suspend")
    override fun suspendSession(
        @PathVariable sessionId: String,
        @RequestBody request: ClientEventRequest
    ) {
        chatEventService.appendLifecycle(
            LifecycleCommand(sessionId, request.clientEventId, SessionStatus.SUSPENDED))
    }

    @PostMapping("/{sessionId}/end")
    override fun endSession(
        @PathVariable sessionId: String,
        @RequestBody request: ClientEventRequest
    ) {
        chatEventService.appendLifecycle(
            LifecycleCommand(sessionId, request.clientEventId, SessionStatus.ENDED))
    }

    @GetMapping
    override fun listSessions(
        @RequestParam(required = false) status: SessionStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): SessionPageResponse =
        sessionQueryService.getSessions(status, from, to, cursor, limit)

    @GetMapping("/{sessionId}")
    override fun getSession(@PathVariable sessionId: String): SessionDetailResponse =
        sessionQueryService.getSessionDetail(sessionId)

    @GetMapping("/{sessionId}/timeline")
    override fun getTimeline(
        @PathVariable sessionId: String,
        @RequestParam(required = false) at: Instant?
    ): TimelineResponse {
        // 1) 이벤트 조회 — 트랜잭션/커넥션은 여기까지만 점유
        val events = sessionQueryService.loadTimelineEvents(sessionId, at)
        // 2) fold(replay) — 트랜잭션 밖, 커넥션 미점유
        return timelineRestoreService.restore(events)
    }
}

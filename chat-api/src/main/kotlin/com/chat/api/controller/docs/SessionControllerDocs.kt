package com.chat.api.controller.docs

import com.chat.api.dto.ClientEventRequest
import com.chat.api.dto.SessionCreateRequest
import com.chat.api.dto.SessionCreateResponse
import com.chat.api.dto.UserSessionRequest
import com.chat.application.session.SessionDetailResponse
import com.chat.application.session.SessionPageResponse
import com.chat.application.session.TimelineResponse
import com.chat.domain.session.SessionStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Instant
import java.time.LocalDateTime

@Tag(name = "Session", description = "세션 생성/참여/퇴장/중단/종료, 목록·단건 조회, 시점 복원")
interface SessionControllerDocs {

    @Operation(
        summary = "세션 생성",
        description = "새 세션을 생성하고 생성자를 참여시킨다. LifecycleEvent(ACTIVE) + UserEvent(JOINED) 를 한 트랜잭션에서 발행한다."
    )
    fun createSession(request: SessionCreateRequest): SessionCreateResponse

    @Operation(summary = "세션 참여", description = "사용자를 세션에 참여시킨다(UserEvent JOINED). 종료(ENDED)된 세션은 거부.")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 세션")
    @ApiResponse(responseCode = "409", description = "이미 종료된 세션 또는 중복 clientEventId")
    fun joinSession(
        @Parameter(description = "세션 ID") sessionId: String,
        request: UserSessionRequest
    )

    @Operation(summary = "세션 퇴장", description = "사용자를 세션에서 퇴장시킨다(UserEvent LEFT).")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 세션")
    @ApiResponse(responseCode = "409", description = "중복 clientEventId")
    fun leaveSession(
        @Parameter(description = "세션 ID") sessionId: String,
        request: UserSessionRequest
    )

    @Operation(summary = "세션 중단", description = "세션을 SUSPENDED 로 전환한다(LifecycleEvent).")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 세션")
    @ApiResponse(responseCode = "409", description = "이미 종료된 세션 또는 중복 clientEventId")
    fun suspendSession(
        @Parameter(description = "세션 ID") sessionId: String,
        request: ClientEventRequest
    )

    @Operation(summary = "세션 종료", description = "세션을 ENDED(terminal) 로 전환한다. 이후 lifecycle/join 은 거부된다.")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 세션")
    @ApiResponse(responseCode = "409", description = "이미 종료된 세션 또는 중복 clientEventId")
    fun endSession(
        @Parameter(description = "세션 ID") sessionId: String,
        request: ClientEventRequest
    )

    @Operation(
        summary = "세션 목록 조회",
        description = "status/기간 필터 + sessionId 커서 페이징(최신순). 모든 파라미터는 선택적. count/OFFSET 없음."
    )
    fun listSessions(
        @Parameter(description = "세션 상태 필터 (ACTIVE/SUSPENDED/ENDED)") status: SessionStatus?,
        @Parameter(description = "시작 시각 하한(이상), ISO-8601") from: LocalDateTime?,
        @Parameter(description = "시작 시각 상한(이하), ISO-8601") to: LocalDateTime?,
        @Parameter(description = "커서: 이 sessionId 보다 과거(작은) 세션부터") cursor: String?,
        @Parameter(description = "페이지 크기 (1~100)", example = "20") limit: Int
    ): SessionPageResponse

    @Operation(summary = "세션 단건 조회", description = "세션 상태 + 참여 이력(현재 활성 + 퇴장) 전체를 반환.")
    @ApiResponse(responseCode = "404", description = "세션 없음")
    fun getSession(@Parameter(description = "세션 ID") sessionId: String): SessionDetailResponse

    @Operation(
        summary = "시점 복원 (timeline)",
        description = "events 를 seq 순으로 fold 해서 at 시점(미지정 시 현재)의 참여자/메시지 상태를 결정적으로 복원한다."
    )
    @ApiResponse(responseCode = "404", description = "존재하지 않는 세션")
    fun getTimeline(
        @Parameter(description = "세션 ID") sessionId: String,
        @Parameter(description = "복원 기준 시각(ISO-8601 instant). 없으면 전체 fold", example = "2026-05-28T01:00:00Z") at: Instant?
    ): TimelineResponse
}

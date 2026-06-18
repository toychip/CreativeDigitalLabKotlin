package com.chat.api.controller.docs

import com.chat.application.message.MessageDirection
import com.chat.application.message.MessagePageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Message", description = "메시지 조회 (read model 커서 페이징)")
interface MessageControllerDocs {

    @Operation(
        summary = "메시지 커서 페이징 조회",
        description = "read model 직접 조회. 커서(=seq) 미지정 시 최신부터, BEFORE=과거 방향, AFTER=최신 방향. count/OFFSET 없음. 실시간 수신은 WebSocket, 과거 조회는 이 API 2경로."
    )
    @ApiResponse(responseCode = "404", description = "존재하지 않는 세션")
    fun getMessages(
        @Parameter(description = "세션 ID") sessionId: String,
        @Parameter(description = "커서(seq). 없으면 최신부터") cursor: Long?,
        @Parameter(description = "페이지 크기 (1~100)", example = "50") limit: Int,
        @Parameter(description = "조회 방향 (BEFORE/AFTER)", example = "BEFORE") direction: MessageDirection
    ): MessagePageResponse
}

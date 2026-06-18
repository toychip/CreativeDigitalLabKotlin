package com.chat.api.controller.docs

import com.chat.api.dto.UserCreateRequest
import com.chat.application.user.UserCreateResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User", description = "회원 등록 (인증 비목표 — userId 식별만)")
interface UserControllerDocs {

    @Operation(
        summary = "회원 등록",
        description = "클라가 지정한 userId(unique) + username(중복 허용)로 회원을 생성한다. 서버 PK(id)는 내부 발급. WebSocket ?userId=, senderId 가 이 userId 를 참조."
    )
    @ApiResponse(responseCode = "409", description = "이미 사용 중인 userId")
    fun createUser(request: UserCreateRequest): UserCreateResponse
}

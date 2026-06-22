package com.chat.api.dto

import jakarta.validation.constraints.NotBlank

/** join / leave 공통 요청 */
data class UserSessionRequest(
    @field:NotBlank val userId: String,
    @field:NotBlank val clientEventId: String
)

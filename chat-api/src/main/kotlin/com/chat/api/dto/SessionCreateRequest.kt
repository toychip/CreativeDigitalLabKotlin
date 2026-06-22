package com.chat.api.dto

import jakarta.validation.constraints.NotBlank

data class SessionCreateRequest(
    @field:NotBlank val creatorUserId: String,
    @field:NotBlank val clientEventId: String
)

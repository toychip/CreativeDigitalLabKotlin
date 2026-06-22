package com.chat.api.dto

import jakarta.validation.constraints.NotBlank

data class UserCreateRequest(
    @field:NotBlank val userId: String,
    @field:NotBlank val username: String
)

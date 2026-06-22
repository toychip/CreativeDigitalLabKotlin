package com.chat.api.dto

import jakarta.validation.constraints.NotBlank

/** suspend / end 공통 요청 */
data class ClientEventRequest(
    @field:NotBlank val clientEventId: String
)

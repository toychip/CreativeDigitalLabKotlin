package com.chat.api.dto

/** join / leave 공통 요청 */
data class UserSessionRequest(val userId: String, val clientEventId: String)

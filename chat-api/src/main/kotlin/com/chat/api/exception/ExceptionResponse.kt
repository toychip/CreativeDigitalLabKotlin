package com.chat.api.exception

data class ExceptionResponse(
    val message: String?,
    val cause: String?,
    val timestamp: Long
) {
    companion object {
        fun of(message: String?, cause: String?): ExceptionResponse =
            ExceptionResponse(message, cause, System.currentTimeMillis())
    }
}

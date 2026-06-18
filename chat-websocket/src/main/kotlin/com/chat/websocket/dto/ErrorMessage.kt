package com.chat.websocket.dto

import com.chat.domain.exception.CdlException

data class ErrorMessage(
    val code: String,
    val message: String,
    val detail: String?,
    val clientEventId: String?
) {
    companion object {
        fun from(e: CdlException, clientEventId: String?): ErrorMessage =
            ErrorMessage(e.code.name, e.defaultMessage(), e.detail, clientEventId)
    }
}

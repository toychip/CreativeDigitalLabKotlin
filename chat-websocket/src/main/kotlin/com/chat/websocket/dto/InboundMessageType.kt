package com.chat.websocket.dto

import com.chat.domain.exception.CdlException
import com.chat.domain.exception.ExceptionCode

enum class InboundMessageType {
    SEND_MESSAGE,
    EDIT_MESSAGE,
    DELETE_MESSAGE,
    HEALTHCHECK

    ;

    companion object {
        fun parseType(typeStr: String?): InboundMessageType {
            if (typeStr == null) {
                throw CdlException(ExceptionCode.UNKNOWN_MESSAGE_TYPE)
            }
            return try {
                InboundMessageType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                throw CdlException(ExceptionCode.UNKNOWN_MESSAGE_TYPE, typeStr)
            }
        }
    }
}

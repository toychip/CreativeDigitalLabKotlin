package com.chat.domain.exception

class CdlException : RuntimeException {

    val code: ExceptionCode
    val detail: String?

    constructor(code: ExceptionCode) : this(code, null)

    constructor(code: ExceptionCode, detail: String?) : super(
        if (detail != null) "[$code] $detail" else "[$code] ${code.defaultMessage}"
    ) {
        this.code = code
        this.detail = detail
    }

    fun statusCode(): Int? = code.statusCode

    fun defaultMessage(): String = code.defaultMessage
}

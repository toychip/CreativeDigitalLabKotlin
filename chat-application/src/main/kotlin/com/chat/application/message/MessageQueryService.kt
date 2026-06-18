package com.chat.application.message

interface MessageQueryService {

    fun getMessagesByCursor(sessionId: String, cursor: Long?, limit: Int, direction: MessageDirection): MessagePageResponse
}

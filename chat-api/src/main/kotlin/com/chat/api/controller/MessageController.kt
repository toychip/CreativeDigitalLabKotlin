package com.chat.api.controller

import com.chat.api.controller.docs.MessageControllerDocs
import com.chat.application.message.MessageDirection
import com.chat.application.message.MessagePageResponse
import com.chat.application.message.MessageQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sessions/{sessionId}/messages")
class MessageController(
    private val messageQueryService: MessageQueryService
) : MessageControllerDocs {

    @GetMapping("/cursor")
    override fun getMessages(
        @PathVariable sessionId: String,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(defaultValue = "BEFORE") direction: MessageDirection
    ): MessagePageResponse =
        messageQueryService.getMessagesByCursor(sessionId, cursor, limit, direction)
}

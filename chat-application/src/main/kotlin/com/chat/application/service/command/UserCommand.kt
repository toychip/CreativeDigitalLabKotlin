package com.chat.application.service.command

import com.chat.domain.event.UserEvent

data class UserCommand(
    val sessionId: String,
    val clientEventId: String,
    val userId: String,
    val type: UserEvent.Type
)

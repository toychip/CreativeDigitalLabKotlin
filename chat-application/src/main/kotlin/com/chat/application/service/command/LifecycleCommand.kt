package com.chat.application.service.command

import com.chat.domain.session.SessionStatus

data class LifecycleCommand(
    val sessionId: String,
    val clientEventId: String,
    val status: SessionStatus
)

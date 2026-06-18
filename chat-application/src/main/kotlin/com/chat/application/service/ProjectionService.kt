package com.chat.application.service

import com.chat.domain.event.LifecycleEvent
import com.chat.domain.event.MessageEvent
import com.chat.domain.event.UserEvent
import org.springframework.transaction.annotation.Transactional

interface ProjectionService {
    @Transactional
    fun handleLifecycle(event: LifecycleEvent)

    @Transactional
    fun handleUser(event: UserEvent)

    @Transactional
    fun handleMessage(event: MessageEvent)
}

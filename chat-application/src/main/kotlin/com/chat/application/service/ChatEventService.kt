package com.chat.application.service

import com.chat.application.service.command.LifecycleCommand
import com.chat.application.service.command.MessageCommand
import com.chat.application.service.command.UserCommand
import com.chat.domain.event.LifecycleEvent
import com.chat.domain.event.MessageEvent
import com.chat.domain.event.UserEvent
import java.util.Optional

interface ChatEventService {

    // 세션 생성: LifecycleEvent(ACTIVE) + 생성자 UserEvent(JOINED) 를 한 트랜잭션으로 원자 발행.
    fun createSession(sessionId: String, clientEventId: String, creatorUserId: String)

    fun appendLifecycle(command: LifecycleCommand): Optional<LifecycleEvent>

    fun appendUser(command: UserCommand): Optional<UserEvent>

    fun appendMessage(command: MessageCommand): Optional<MessageEvent>
}

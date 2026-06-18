package com.chat.application.service

import com.chat.application.event.EventEntity
import com.chat.application.event.EventRepository
import com.chat.application.sequence.SequenceGenerator
import com.chat.application.service.command.LifecycleCommand
import com.chat.application.service.command.MessageCommand
import com.chat.application.service.command.UserCommand
import com.chat.application.session.SessionRepository
import com.chat.domain.event.LifecycleEvent
import com.chat.domain.event.MessageEvent
import com.chat.domain.event.UserEvent
import com.chat.domain.exception.CdlException
import com.chat.domain.exception.ExceptionCode
import com.chat.domain.session.SessionStatus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
class ChatEventServiceImpl(
    private val eventRepository: EventRepository,
    private val sequenceGenerator: SequenceGenerator,
    private val eventPublisher: ApplicationEventPublisher,
    private val sessionRepository: SessionRepository
) : ChatEventService {

    @Transactional
    override fun createSession(sessionId: String, clientEventId: String, creatorUserId: String) {
        val lifecycleSeq = sequenceGenerator.nextSeq(sessionId)
        val lifecycle = LifecycleEvent.create(
            sessionId, clientEventId, lifecycleSeq, SessionStatus.ACTIVE
        )
        eventRepository.saveAndFlush(EventEntity.from(lifecycle))

        val userSeq = sequenceGenerator.nextSeq(sessionId)
        val user = UserEvent.create(
            sessionId, clientEventId, userSeq, creatorUserId, UserEvent.Type.JOINED
        )
        eventRepository.saveAndFlush(EventEntity.from(user))

        eventPublisher.publishEvent(lifecycle)
        eventPublisher.publishEvent(user)
    }

    @Transactional
    override fun appendLifecycle(command: LifecycleCommand): Optional<LifecycleEvent> {
        ensureSessionExists(command.sessionId)
        ensureNotEnded(command.sessionId)
        val seq = sequenceGenerator.nextSeq(command.sessionId)
        val event = LifecycleEvent.create(
            command.sessionId, command.clientEventId, seq, command.status
        )
        saveOrThrowDuplicate(EventEntity.from(event))
        eventPublisher.publishEvent(event)
        return Optional.of(event)
    }

    @Transactional
    override fun appendUser(command: UserCommand): Optional<UserEvent> {
        ensureSessionExists(command.sessionId)
        if (command.type == UserEvent.Type.JOINED) {
            ensureNotEnded(command.sessionId)
        }
        val seq = sequenceGenerator.nextSeq(command.sessionId)
        val event = UserEvent.create(
            command.sessionId, command.clientEventId, seq,
            command.userId, command.type
        )
        saveOrThrowDuplicate(EventEntity.from(event))
        eventPublisher.publishEvent(event)
        return Optional.of(event)
    }

    @Transactional
    override fun appendMessage(command: MessageCommand): Optional<MessageEvent> {
        val seq = sequenceGenerator.nextSeq(command.sessionId)
        val event = MessageEvent.create(
            command.sessionId, command.clientEventId, seq,
            command.senderId, command.messageId, command.content, command.type
        )
        saveOrThrowDuplicate(EventEntity.from(event))
        eventPublisher.publishEvent(event)
        return Optional.of(event)
    }

    // 멱등성: DB 유니크 제약(uk_events_session_client) 동시/스케일아웃 재전송도 여기서 잡힘
    private fun saveOrThrowDuplicate(entity: EventEntity) {
        try {
            eventRepository.saveAndFlush(entity)
        } catch (e: DataIntegrityViolationException) {
            throw CdlException(ExceptionCode.DUPLICATE_EVENT)
        }
    }

    // 세션 존재 확인 — events(진실의 원천) 기준이라 생성 직후 projection 지연과 무관
    private fun ensureSessionExists(sessionId: String) {
        if (!eventRepository.existsBySessionId(sessionId)) {
            throw CdlException(ExceptionCode.SESSION_NOT_FOUND)
        }
    }

    // ENDED 세션에 추가 lifecycle/join 차단 (read model 기준)
    private fun ensureNotEnded(sessionId: String) {
        sessionRepository.findById(sessionId).ifPresent { session ->
            if (session.status == SessionStatus.ENDED) {
                throw CdlException(ExceptionCode.SESSION_ALREADY_ENDED)
            }
        }
    }
}

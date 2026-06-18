package com.chat.application.service

import com.chat.application.message.MessageEntity
import com.chat.application.message.MessageRepository
import com.chat.application.session.SessionEntity
import com.chat.application.session.SessionRepository
import com.chat.application.sessionuser.MemberRole
import com.chat.application.sessionuser.SessionUserEntity
import com.chat.application.sessionuser.SessionUserRepository
import com.chat.domain.event.LifecycleEvent
import com.chat.domain.event.MessageEvent
import com.chat.domain.event.UserEvent
import com.chat.domain.session.MessageStatus
import com.chat.domain.session.SessionStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectionServiceImpl(
    private val sessionRepository: SessionRepository,
    private val sessionUserRepository: SessionUserRepository,
    private val messageRepository: MessageRepository
) : ProjectionService {

    @Transactional
    override fun handleLifecycle(event: LifecycleEvent) {
        val session = sessionRepository.findById(event.sessionId).orElse(null)

        if (session == null) {
            createSessionIfActive(event)
            return
        }

        if (session.status == SessionStatus.ENDED) {
            log.warn("Lifecycle event on ENDED session, ignoring. sessionId={}, incomingStatus={}",
                event.sessionId, event.status)
            return
        }

        session.changeStatus(event.status)
    }

    private fun createSessionIfActive(event: LifecycleEvent) {
        if (event.status != SessionStatus.ACTIVE) {
            log.warn("No session found and status is not ACTIVE, ignoring. sessionId={}, status={}",
                event.sessionId, event.status)
            return
        }
        sessionRepository.save(SessionEntity.start(event.sessionId))
    }

    @Transactional
    override fun handleUser(event: UserEvent) {
        when (event.type) {
            UserEvent.Type.JOINED -> handleUserJoined(event)
            UserEvent.Type.LEFT -> sessionUserRepository.leaveSession(event.sessionId, event.userId)
        }
    }

    private fun handleUserJoined(event: UserEvent) {
        val alreadyActive = sessionUserRepository.existsBySessionIdAndUserIdAndIsActiveTrue(
            event.sessionId, event.userId)
        if (alreadyActive) {
            return
        }

        val previous = sessionUserRepository
            .findBySessionIdAndUserId(event.sessionId, event.userId)
            .orElse(null)

        if (previous != null) {
            previous.rejoin()
            return
        }

        sessionUserRepository.save(
            SessionUserEntity.join(event.sessionId, event.userId, MemberRole.MEMBER))
    }

    @Transactional
    override fun handleMessage(event: MessageEvent) {
        when (event.type) {
            MessageStatus.SENT -> handleMessageSent(event)
            MessageStatus.EDITED -> handleMessageEdited(event)
            MessageStatus.DELETED -> handleMessageDeleted(event)
        }
    }

    private fun handleMessageSent(event: MessageEvent) {
        if (messageRepository.existsById(event.messageId)) {
            return
        }
        messageRepository.save(MessageEntity.send(
            event.messageId, event.sessionId, event.senderId,
            event.content, event.seq))
    }

    private fun handleMessageEdited(event: MessageEvent) {
        val message = messageRepository.findById(event.messageId).orElse(null)
        if (message == null) {
            // TODO Production 환경에서 트래픽 파악하여 재시도 규칙 설정 (순서 역전으로 SENT 미도착 시 재시도)
            log.warn("EDITED event for unknown messageId={}", event.messageId)
            return
        }
        message.edit(event.content)
    }

    private fun handleMessageDeleted(event: MessageEvent) {
        val message = messageRepository.findById(event.messageId).orElse(null)
        if (message == null) {
            // TODO Production 환경에서 트래픽 파악하여 재시도 규칙 설정 (순서 역전으로 SENT 미도착 시 재시도)
            log.warn("DELETED event for unknown messageId={}", event.messageId)
            return
        }
        message.delete()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProjectionServiceImpl::class.java)
    }
}

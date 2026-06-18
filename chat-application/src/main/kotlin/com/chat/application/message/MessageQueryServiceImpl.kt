package com.chat.application.message

import com.chat.application.session.SessionRepository
import com.chat.domain.exception.CdlException
import com.chat.domain.exception.ExceptionCode
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MessageQueryServiceImpl(
    private val messageRepository: MessageRepository,
    private val sessionRepository: SessionRepository
) : MessageQueryService {

    @Transactional(readOnly = true)
    override fun getMessagesByCursor(sessionId: String, cursor: Long?, limit: Int, direction: MessageDirection): MessagePageResponse {
        if (!sessionRepository.existsById(sessionId)) {
            throw CdlException(ExceptionCode.SESSION_NOT_FOUND)
        }
        val capped = maxOf(1, minOf(limit, MAX_LIMIT))
        val pageLimit = Limit.of(capped)

        val messages = fetch(sessionId, cursor, direction, pageLimit)
        val views = messages.map { MessageView.from(it) }

        return MessagePageResponse.of(views, cursor, capped)
    }

    private fun fetch(sessionId: String, cursor: Long?, direction: MessageDirection, limit: Limit): List<MessageEntity> {
        if (cursor == null) {
            return messageRepository.findBySessionIdOrderBySeqDesc(sessionId, limit)
        }
        if (direction == MessageDirection.BEFORE) {
            return messageRepository.findBySessionIdAndSeqLessThanOrderBySeqDesc(sessionId, cursor, limit)
        }
        val ascending =
            messageRepository.findBySessionIdAndSeqGreaterThanOrderBySeqAsc(sessionId, cursor, limit)
        return ascending.reversed()
    }

    companion object {
        private const val MAX_LIMIT = 100
    }
}

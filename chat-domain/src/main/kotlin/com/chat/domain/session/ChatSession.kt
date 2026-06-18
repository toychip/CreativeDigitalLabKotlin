package com.chat.domain.session

import com.chat.domain.event.ChatEvent
import com.chat.domain.event.LifecycleEvent
import com.chat.domain.event.MessageEvent
import com.chat.domain.event.UserEvent
import java.util.Collections

/**
 * 채팅 세션이 지금 어떤 상태인지를 나타내는 도메인 객체
 * ChatEvent들을 처음부터 차례로 적용해서 만들어진다.
 * DB에 따로 저장되지 않고, 시점 복원이 필요할 때마다 events 테이블에서 이벤트들을 가져와 재구성한다.
 */
class ChatSession {

    var sessionId: String? = null
        private set
    var status: SessionStatus? = null
        private set
    var lastSeq: Long = 0
        private set
    private val participants = LinkedHashSet<String>()
    private val messages = LinkedHashMap<String, Message>()

    fun apply(event: ChatEvent) {
        when (event) {
            is LifecycleEvent -> applyLifecycle(event)
            is UserEvent -> applyUser(event)
            is MessageEvent -> applyMessage(event)
        }
        this.lastSeq = event.seq
    }

    private fun applyLifecycle(e: LifecycleEvent) {
        this.sessionId = e.sessionId
        this.status = e.status
    }

    private fun applyUser(e: UserEvent) {
        when (e.type) {
            UserEvent.Type.JOINED -> participants.add(e.userId)
            UserEvent.Type.LEFT -> participants.remove(e.userId)
        }
    }

    private fun applyMessage(e: MessageEvent) {
        when (e.type) {
            MessageStatus.SENT -> messages[e.messageId] =
                Message(e.messageId, e.senderId, e.content, MessageStatus.SENT)

            MessageStatus.EDITED -> {
                val existing = messages[e.messageId]
                if (existing != null) {
                    messages[e.messageId] = existing.withContent(e.content)
                }
            }

            MessageStatus.DELETED -> {
                val existing = messages[e.messageId]
                if (existing != null) {
                    messages[e.messageId] = existing.markDeleted()
                }
            }
        }
    }

    fun getParticipants(): Set<String> =
        Collections.unmodifiableSet(LinkedHashSet(participants))

    fun getMessages(): Collection<Message> =
        messages.values.toList()

    companion object {
        // fold
        fun loadFromEvents(events: List<ChatEvent>): ChatSession {
            val session = ChatSession()
            events.forEach(session::apply)
            return session
        }
    }
}

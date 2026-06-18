package com.chat.application.event

import com.chat.application.common.JsonUtil
import com.chat.domain.event.ChatEvent
import com.chat.domain.event.LifecycleEvent
import com.chat.domain.event.MessageEvent
import com.chat.domain.event.UserEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "events",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_events_session_client", columnNames = ["session_id", "client_event_id", "event_type"]),
        UniqueConstraint(name = "uk_events_session_seq", columnNames = ["session_id", "seq"])
    ],
    indexes = [
        Index(name = "idx_events_session_created", columnList = "session_id, created_at")
    ]
)
class EventEntity private constructor(
    @Id
    @Column(name = "event_id", length = 36, nullable = false, updatable = false)
    val eventId: String,

    @Column(name = "session_id", length = 36, nullable = false, updatable = false)
    val sessionId: String,

    @Column(name = "client_event_id", length = 36, nullable = false, updatable = false)
    val clientEventId: String,

    @Column(name = "seq", nullable = false, updatable = false)
    val seq: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20, nullable = false, updatable = false)
    val eventType: EventType,

    @Column(name = "payload", columnDefinition = "JSON", nullable = false, updatable = false)
    val payload: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant
) {

    // entity -> domain event
    fun toDomain(): ChatEvent =
        when (eventType) {
            EventType.LIFECYCLE -> JsonUtil.fromJson(payload, LifecycleEvent::class.java)
            EventType.USER -> JsonUtil.fromJson(payload, UserEvent::class.java)
            EventType.MESSAGE -> JsonUtil.fromJson(payload, MessageEvent::class.java)
        }

    companion object {
        // domain event -> entity
        fun from(event: ChatEvent): EventEntity {
            val type = when (event) {
                is LifecycleEvent -> EventType.LIFECYCLE
                is UserEvent -> EventType.USER
                is MessageEvent -> EventType.MESSAGE
            }
            return EventEntity(
                event.eventId,
                event.sessionId,
                event.clientEventId,
                event.seq,
                type,
                JsonUtil.toJson(event),
                event.createdAt
            )
        }
    }
}

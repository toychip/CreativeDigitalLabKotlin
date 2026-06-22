package com.chat.application.session

import com.chat.application.sessionuser.MemberRole
import com.chat.application.sessionuser.SessionUserEntity
import java.time.LocalDateTime

data class ParticipantView(
    val userId: String,
    val role: MemberRole,
    val active: Boolean,
    val online: Boolean,
    val joinedAt: LocalDateTime,
    val leftAt: LocalDateTime?
) {
    companion object {
        fun from(entity: SessionUserEntity, online: Boolean): ParticipantView =
            ParticipantView(
                entity.userId,
                entity.role,
                entity.isActive,
                online,
                entity.joinedAt,
                entity.leftAt
            )
    }
}

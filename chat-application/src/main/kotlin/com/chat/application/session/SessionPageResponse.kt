package com.chat.application.session

data class SessionPageResponse(
    val sessions: List<SessionView>,
    val nextCursor: String?,
    val hasNext: Boolean
) {
    companion object {
        /** 결과는 sessionId 내림차순(최신 먼저). nextCursor = 마지막(가장 과거) sessionId */
        fun of(sessions: List<SessionEntity>, limit: Int): SessionPageResponse {
            val views = sessions.map { SessionView.from(it) }
            if (views.isEmpty()) {
                return SessionPageResponse(views, null, false)
            }
            return SessionPageResponse(views, views.last().sessionId, views.size == limit)
        }
    }
}

package com.chat.application.message

data class MessagePageResponse(
    val messages: List<MessageView>,
    val nextCursor: Long?,
    val prevCursor: Long?,
    val hasNext: Boolean,
    val hasPrev: Boolean
) {
    companion object {
        /**
         * 조회 결과는 항상 seq 내림차순(최신 먼저)
         * nextCursor = 마지막(가장 과거) seq, prevCursor = 첫(가장 최신) seq
         * hasNext = 요청 limit 만큼 꽉 찼는지, hasPrev = 커서가 있었는지
         */
        fun of(views: List<MessageView>, requestCursor: Long?, limit: Int): MessagePageResponse {
            if (views.isEmpty()) {
                return MessagePageResponse(views, null, null, false, requestCursor != null)
            }
            return MessagePageResponse(
                views,
                views.last().seq,
                views.first().seq,
                views.size == limit,
                requestCursor != null
            )
        }
    }
}

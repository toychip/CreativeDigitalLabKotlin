package com.chat.domain.session

/**
 * 메시지 한 건
 * MessageEvent 를 누적 적용한 결과로 만들어지며, ChatSession 이 보관한다.
 */
data class Message(
    val messageId: String,
    val senderId: String,
    val content: String?,
    val status: MessageStatus
) {

    fun withContent(newContent: String?): Message =
        Message(messageId, senderId, newContent, MessageStatus.EDITED)

    fun markDeleted(): Message =
        Message(messageId, senderId, content, MessageStatus.DELETED)
}

package com.chat.domain.session

/**
 * 메시지의 상태값
 * MessageEvent의 type 으로도 쓰고, MessageState 의 현재 상태로도 쓴다.
 * (이벤트의 종류와 결과 상태가 1:1 매핑이라 통합)
 */
enum class MessageStatus {
    SENT,
    EDITED,
    DELETED
}

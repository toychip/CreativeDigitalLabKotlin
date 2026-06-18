package com.chat.domain.exception

/**
 * 전 모듈 공통 예외 코드. statusCode 는 REST 응답용 — WebSocket 등 HTTP 무관 예외는 null.
 */
enum class ExceptionCode(
    val statusCode: Int?,
    val defaultMessage: String
) {

    // 404
    SESSION_NOT_FOUND(404, "세션을 찾을 수 없습니다."),
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),

    // 409
    SESSION_ALREADY_ENDED(409, "이미 종료된 세션입니다."),
    USER_ID_ALREADY_TAKEN(409, "이미 사용 중인 userId 입니다."),
    DUPLICATE_EVENT(409, "이미 처리된 이벤트입니다 (clientEventId 중복)."),

    // WebSocket — HTTP 무관 (statusCode null, sendError 로 전달)
    INVALID_MESSAGE_FORMAT(null, "메시지 형식이 올바르지 않습니다."),
    SESSION_ID_REQUIRED(null, "sessionId 가 필요합니다."),
    CLIENT_EVENT_ID_REQUIRED(null, "clientEventId 가 필요합니다."),
    MESSAGE_ID_REQUIRED(null, "messageId 가 필요합니다."),
    UNKNOWN_MESSAGE_TYPE(null, "알 수 없는 메시지 타입입니다.");
}

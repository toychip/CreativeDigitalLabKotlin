package com.chat.application.presence

interface PresenceService {

    /** WS 연결/HEALTHCHECK 하트비트 — presence TTL 갱신(online 유지) */
    fun heartbeat(userId: String)

    /** 정상 종료 시 즉시 offline 처리 */
    fun markOffline(userId: String)

    /** 현재 online 여부 (presence 키 존재 = TTL 내 하트비트 수신) */
    fun isOnline(userId: String): Boolean
}

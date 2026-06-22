package com.chat.application.presence

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class PresenceServiceImpl(
    private val redisTemplate: RedisTemplate<String, String>
) : PresenceService {

    // 연결 시점 + 매 HEALTHCHECK 마다 TTL 리셋. TTL 안에 다음 하트비트가 안 오면(크래시/무응답) 키가 자동 만료 = offline.
    // 인스턴스 메모리가 아니라 Redis 라 어느 인스턴스에 붙어도 동일하게 판정된다(분산 presence).
    override fun heartbeat(userId: String) {
        redisTemplate.opsForValue().set(key(userId), ONLINE_MARKER, PRESENCE_TTL)
    }

    override fun markOffline(userId: String) {
        redisTemplate.delete(key(userId))
    }

    override fun isOnline(userId: String): Boolean =
        redisTemplate.hasKey(key(userId)) == true

    private fun key(userId: String): String = KEY_PREFIX + userId

    companion object {
        private const val KEY_PREFIX = "presence:"
        private const val ONLINE_MARKER = "1"

        // 하트비트(권장 ~10s) 약 3회 누락까지 허용
        private val PRESENCE_TTL: Duration = Duration.ofSeconds(30)
    }
}

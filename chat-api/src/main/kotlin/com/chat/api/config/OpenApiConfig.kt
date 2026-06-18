package com.chat.api.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "chat-cdl API",
        version = "v1",
        description = "이벤트 소싱 기반 1:1 실시간 채팅 — 세션 lifecycle, 이벤트 수집/조회, 시점 복원 REST API. " +
            "실시간 메시지 송수신은 WebSocket(/ws/chat) — README 참고."
    )
)
class OpenApiConfig

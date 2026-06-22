# CreativeDigitalLab 과제 레포지토리

# 설계 문서

실시간 1:1 채팅 + 이벤트 기반 상태 복원 시스템 설계 메모. 구현한 것과 설계만 한 것을 구분. DB 는 ERD.md, 분산 중복 한계는 중복한계.md

## 실행 방법

Linux / macOS 환경, 프로젝트 루트에서

```bash
./start-cluster.sh
```

## 기술 스택

- Java 21 — 이벤트 타입을 sealed interface + switch 로 망라. 한 종류라도 누락하면 컴파일 에러
- Spring Boot 3.x — WebSocket / Data JPA / Redis / Actuator. `@TransactionalEventListener(AFTER_COMMIT)` 로 쓰기와 후처리 분리
- WebSocket: STOMP 없이 raw handler. 프로토콜이 JSON 한 줄이라 STOMP 오버헤드 불필요
- MySQL 8.4 — events append-only + UNIQUE 로 중복 차단, payload 는 JSON
- Redis 7 — Pub/Sub(멀티 인스턴스 전파), INCR(세션별 seq), Set(인스턴스별 구독 목록)
- Gradle 멀티모듈, nginx + 앱 3대 docker-compose, springdoc(OpenAPI)

## 아키텍처

쓰기는 전부 events 에 추가, 읽기는 실시간 통신과 이전 데이터 조회로 나뉨

```
쓰기 ─ ChatEventService → events(append-only)
                              │ AFTER_COMMIT
                              ├→ ProjectionUpdater(@Async) → 읽기 모델 갱신
                              └→ ChatEventListener → 로컬 WS 전송 + Redis publish

읽기 ─ 현재 상태  → 읽기 모델 직접 조회
      특정 시점 t → events 를 t 까지 접어서(fold) 재구성
```

모듈: `chat-api → chat-websocket → chat-application → chat-domain` 단방향 의존. 도메인(이벤트·fold)은 프레임워크 비의존

## 이벤트 모델

`ChatEvent` sealed, 세 종류. 공통 필드: eventId, sessionId, clientEventId(멱등 키), seq(서버 순번), createdAt

| 이벤트 | 내용 |
|---|---|
| LifecycleEvent | 세션 상태 ACTIVE / SUSPENDED / ENDED |
| UserEvent | 참여 / 퇴장 JOINED / LEFT |
| MessageEvent | 메시지 SENT / EDITED / DELETED |

disconnect/reconnect 는 이벤트로 미기록. 접속/끊김은 네트워크 불안정 시 폭증하는 일시적 상태 → 이벤트로 쌓으면 로그 오염. 접속 여부는 presence 로 별도 처리(아래)

## 쓰기 경로

모든 쓰기는 ChatEventService 단일 경유. 세션 생성은 LifecycleEvent(ACTIVE) + UserEvent(JOINED) 를 한 트랜잭션에, join/leave/suspend/end 는 각 이벤트 1건, 메시지는 WebSocket 수신 → MessageEvent. 처리 순서: seq 발급 → events INSERT(같은 트랜잭션 커밋) → publish. 커밋 후에만 후처리가 돌아 롤백된 이벤트는 전파·프로젝션 안 됨

## 중복 처리

같은 메시지 재전송 전제로 4겹(상세는 중복한계.md)

1. clientEventId + DB UNIQUE — 쓰기 중복 차단. 모든 인스턴스가 같은 DB 를 보므로 다른 서버로 재시도해도 차단
2. serverId — 자기가 publish 한 broadcast 를 자기가 받는 self-echo 제거
3. 수신측 envelope.id dedup — 같은 broadcast 가 두 번 와도 한 번만 처리
4. catch-up — 그래도 빠지면 클라가 seq 커서로 재수신

프로젝션에도 멱등 처리(존재 확인 후 INSERT, 종료·삭제 terminal). 한계: 3번 키가 envelope.id 라 재발행 시 새 id 가 붙어 무력 → 진짜 해법은 (sessionId, seq) 키.

## 순서 처리

기준 = 서버 발급 seq(세션별 Redis INCR). 클라 시계가 아니라 서버 수신 순서를 기준으로 삼아 도착 역전·시계 오차와 무관하게 단일 순서 확보. 조회·복원은 전부 ORDER BY seq. fold 에서 EDIT/DELETE 가 대상보다 먼저 와도 대상 부재 시 무시

한계: seq 는 서버 도착 순서 → 클라 의도 순서와 도착 순서가 다르면(EDIT 가 SENT 보다 먼저 도착) 편집 유실 가능. 채팅에선 SENT 응답 후 EDIT 전송으로 회피

## 비동기 프로젝션

events 커밋 직후 AFTER_COMMIT 으로 둘 실행 — 읽기 모델 갱신(ProjectionUpdater, @Async 전용 풀) + 실시간 전파(ChatEventListener). 둘 다 쓰기 트랜잭션 밖이라 실패해도 events 는 안전. 읽기 모델은 events 로 언제든 재생성 가능 → 쓰기 응답을 여기 묶지 않음. 전용 풀(core 4 / max 16 / queue 1000)을 둔 이유: 기본 @Async 는 스레드를 무제한 생성

재시도/DLQ 미구현(현재 실패 시 로그만). 운영 설계: 지수 백오프 재시도 → N회 초과 시 별도 테이블 격리 후 수동/배치 재처리, 큐 포화 시 호출 스레드 직접 실행(역압). 프로젝션이 멱등이라 재처리 안전

## 실시간 전파 & 수평 확장

앱은 거의 무상태. 세션 상태는 DB, seq 는 Redis, 구독 목록은 Redis Set, 인스턴스 메모리에는 그 인스턴스가 받은 WebSocket 연결만

```
유저A → 인스턴스1 전송
   인스턴스1: 자기 연결에 바로 전송 + Redis publish(chat.session.{id})
   인스턴스2(구독 중): 받아서 자기 연결에 전송
   인스턴스3(미구독): 안 받음
```

각 인스턴스는 자기 유저가 속한 세션만 구독. 한 세션의 두 참여자가 다른 인스턴스에 붙어도 Redis 채널로 연결 → sticky session 불필요, nginx 는 단순 라운드로빈. nginx 는 /ws/ 에 Upgrade 헤더 + 긴 read timeout(3600s)만 추가

## 재연결

클라가 아무 인스턴스로 재연결 → 그 인스턴스가 읽기 모델에서 유저의 활성 세션을 찾아 자동 재구독. 끊긴 동안 빠진 메시지는 마지막 seq 이후를 커서로 회수(`GET /sessions/{id}/messages/cursor?direction=AFTER`). 비정상 종료는 handleTransportError + afterConnectionClosed 양쪽에서 연결 정리, 정리가 멱등이라 두 번 불려도 안전(좀비 연결 방지)

## Presence

"참여" 와 "접속" 은 다른 층위. 참여(세션 멤버 여부)는 session_users.is_active 로 영구 보관, 접속(현재 연결 여부)은 일시적이라 Redis presence 키 + users.last_seen_at(마지막 접속 시각)

online/offline 은 HEALTHCHECK 하트비트 기반 분산 presence 로 판정한다. 클라가 WS 로 `{"type":"HEALTHCHECK"}` 를 주기적으로(권장 ~10s) 보내면 서버가 `presence:{userId}` Redis 키의 TTL(30s)을 갱신한다. TTL 안에 다음 하트비트가 안 오면(크래시·무응답) 키가 자동 만료되어 offline. 인스턴스 메모리가 아니라 Redis 라 어느 노드에 붙어도 동일하게 판정된다(README 의 기존 "메모리 기준 한계" 해소). 연결 시 즉시 online, 정상 종료 시 로컬 연결이 모두 사라지면 즉시 offline(비정상 종료는 TTL 로 수렴). HEALTHCHECK 는 채팅이 아닌 휘발성 신호라 events 에 저장하지 않고 last_seen_at DB 쓰기도 트리거하지 않음. online 여부는 세션 단건 조회(`GET /sessions/{id}`) 응답의 참여자 `online` 필드로 노출(조회 시 `EXISTS presence:{userId}`). 상대에게 실시간 push(전이 알림)는 미구현 — 조회 시 판정만. 1:1 이지만 session_users 는 N명 허용 일반 설계, 1:1 은 참여자 2명 케이스로 간주

## 시점 복원

`GET /sessions/{id}/timeline?at=t` → events 를 t 까지 가져와 ChatSession 으로 접어 그 시점의 참여자·메시지(상태 포함)·세션 상태 재구성. 저장 없이 매번 재생 → 항상 동일 결과. 중복은 fold 가 흡수(같은 messageId 덮어쓰기, 참여자 Set), 순서는 seq 정렬

복원 경계도 seq 기준. created_at 은 서버별 시계(클럭 스큐·브로드캐스트 역전)라 신뢰 불가 → 시간(at)으로 바로 자르면 seq 와 어긋나 구멍 난 prefix 가 될 수 있다. 그래서 `max(seq) WHERE created_at<=t` 로 경계 seq 를 정하고 `seq<=경계` 연속 prefix 를 fold → 원자적 seq 기준의 결정적 복원. 조회(@Transactional)와 fold(인메모리, 트랜잭션 밖 TimelineRestoreService)도 분리해 fold 가 길어져도 커넥션을 점유하지 않음

## 쿼리 최적화

메시지 스크롤:
```sql
SELECT * FROM messages WHERE session_id=? AND seq<? ORDER BY seq DESC LIMIT ?;
```
idx_message_session_seq 사용. OFFSET 대신 seq 커서 → 깊은 페이지에서도 비용 일정, COUNT 미사용

시점 복원 (2단계 — 시간으로 경계 seq 만 찾고, 복원은 seq prefix):
```sql
-- 1) 경계 seq (idx_events_session_created 범위 스캔)
SELECT MAX(seq) FROM events WHERE session_id=? AND created_at<=?;
-- 2) seq 연속 prefix (uk_events_session_seq 활용)
SELECT * FROM events WHERE session_id=? AND seq<=? ORDER BY seq;
```
created_at 은 서버별 시계라 신뢰 불가 → 경계 탐색에만 쓰고 복원은 원자적 seq 기준. 이벤트 누적 시 스냅샷으로 단축

세션 목록: status·기간 필터 + PK 커서(session_id < cursor). UUID v7 시간순 → 별도 정렬 인덱스 없이 최신순

조회 서비스의 `@Transactional(readOnly=true)` 는 개선 여지. MySQL+JPA 에선 readOnly 트랜잭션이 set/commit 류 부가 쿼리를 DB 로 추가 전송 → 단건 조회에서 왕복만 증가. 단순 조회는 트랜잭션 제거 또는 propagation=SUPPORTS 적용을 부하 테스트 후 검토

## 관측

현재: actuator(health/info/metrics) + docker health check, nginx 접근 로그(인스턴스·응답 시간), 애플리케이션 로그. 향후: Micrometer→Prometheus 로 이벤트 처리량·프로젝션 지연·dedup 적중률 지표, 로그 traceId 로 한 요청 전 구간 추적

## 장애 대응

서버 다운: docker health check 실패 → nginx 가 죽은 인스턴스 제외. sticky 없어 클라는 다른 인스턴스로 재연결 → 자동 재구독 + catch-up 으로 회복. 진행 중 프로젝션은 graceful shutdown(30s)으로 마무리, 이벤트는 전파 전 커밋되어 유실 없음

DB 장애·성능 저하: HikariCP leak 감지 + connection timeout 으로 fast-fail, 무한 대기 차단. events 가 append-only 라 같은 row update 락 경합 없음(중복은 UNIQUE INSERT 충돌 처리), seq 는 Redis 라 DB 핫스팟 회피. 풀 회복 시 큐의 프로젝션 처리(큐 초과분은 현재 유실 → 재시도/DLQ 필요), 읽기 모델 어긋나면 events 재생

데이터 유실·정합성: 위 4겹으로 차단, 쓰기·프로젝션·전파 분리로 부분 실패가 원본 이벤트 미오염. 빠진 건 events 재생 + catch-up 으로 회복. exactly-once 전달 불가 → at-least-once + 멱등 수신(중복한계.md)

## 고도화 

스냅샷, 프로젝션 재시도/DLQ, outbox, 캐싱, 테스트(프로젝션 멱등, fold 결정성, 장애 주입), 부하 테스트, 관측 고도화. Broker Outbox Pattern 등

## API 문서

- **REST API**: 앱 실행 후 Swagger UI — `http://localhost:8080/swagger-ui.html` (OpenAPI JSON: `http://localhost:8080/v3/api-docs`)
- **WebSocket**: OpenAPI 로 기술할 수 없어 아래에 별도 정리

---

## WebSocket 실시간 메시지 API

### 연결

```
ws://<host>/ws/chat?userId=<userId>
```

- 핸드셰이크 시 `userId` 쿼리 파라미터로 사용자를 식별한다 (인증은 과제 비목표 — 토큰 없음). `userId` 누락 시 핸드셰이크 거부.
- 연결 직후, 해당 사용자가 참여 중인 활성 세션을 자동 구독한다.
- 멀티 인스턴스: 어느 노드에 연결돼도 Redis Pub/Sub fanout 으로 같은 세션 메시지를 수신한다 (그래서 sticky session 이 불필요).

### 클라이언트 → 서버 (인바운드)

네 가지 `type` 을 보낸다. 메시지 3종은 모두 `clientEventId` 로 멱등 처리되어, 네트워크 재전송 시 중복 저장이 차단된다. HEALTHCHECK 는 presence 하트비트라 sessionId/clientEventId 가 필요 없고 events 에 저장되지 않는다.

메시지 전송
```json
{ "type": "SEND_MESSAGE", "sessionId": "<sessionId>", "clientEventId": "<uuid>", "content": "안녕하세요" }
```

메시지 수정
```json
{ "type": "EDIT_MESSAGE", "sessionId": "<sessionId>", "clientEventId": "<uuid>", "messageId": "<messageId>", "content": "수정본" }
```

메시지 삭제
```json
{ "type": "DELETE_MESSAGE", "sessionId": "<sessionId>", "clientEventId": "<uuid>", "messageId": "<messageId>" }
```

하트비트 (presence — 주기적으로 전송, 응답 없음)
```json
{ "type": "HEALTHCHECK" }
```

### 서버 → 클라이언트 (아웃바운드)

메시지 이벤트 broadcast (`ChatMessageResponse`)
```json
{
  "messageStatus": "SENT",
  "sessionId": "<sessionId>",
  "seq": 42,
  "createdAt": "2026-05-28T01:00:00Z",
  "clientEventId": "<uuid>",
  "senderId": "<userId>",
  "messageId": "<messageId>",
  "content": "안녕하세요"
}
```

- `messageStatus`: `SENT` | `EDITED` | `DELETED`
- `seq`: 세션 내 서버 발급 순번. 순서 보장·정렬·재연결 catch-up 의 기준.

에러 (`ErrorMessage`)
```json
{ "code": "INVALID_MESSAGE_FORMAT", "message": "메시지 형식이 올바르지 않습니다", "detail": null, "clientEventId": "<uuid>" }
```

### 중복 / 순서 처리 기준

- **중복**: 동일 `(sessionId, clientEventId, eventType)` 는 events 테이블 UNIQUE 제약으로 차단 (재전송 안전).
- **순서**: 서버가 세션별 `seq`(Redis INCR)를 발급. 조회·시점 복원은 `seq` 순 정렬로 fold 하여 결정적(deterministic)으로 복원한다.

### 빠른 테스트 (websocat 예시)

```bash
# 1) 회원 등록 (REST)
curl -X POST localhost:8080/users -H 'Content-Type: application/json' \
  -d '{"userId":"alice","username":"Alice"}'

# 2) 세션 생성 (REST) → 응답의 sessionId 확보
curl -X POST localhost:8080/sessions -H 'Content-Type: application/json' \
  -d '{"creatorUserId":"alice","clientEventId":"create-1"}'

# 3) WebSocket 연결 후 메시지 전송
websocat "ws://localhost:8080/ws/chat?userId=alice"
# (연결되면 아래 JSON 한 줄을 입력)
{"type":"SEND_MESSAGE","sessionId":"<위 sessionId>","clientEventId":"msg-1","content":"hello"}
```

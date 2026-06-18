-- ============================================================
-- chat-cdl :: DB 스키마 (진실의 원천)
-- ============================================================
-- spring.jpa.hibernate.ddl-auto: validate 모드.
-- 본 스키마를 운영자가 수동으로 적용한 뒤 애플리케이션을 부팅한다.
-- (Flyway / Liquibase 미도입 — 1주 과제 스코프)
--
-- 컬럼 타입 정책
--   - CHAR(36):    UUID v7 문자열. BINARY(16) 대비 가독성 우선.
--   - DATETIME(6): 마이크로초 정밀도. 이벤트 정렬/시점 복원 안전성.
--   - JSON:        이벤트 페이로드 (type-specific 직렬화).
-- ============================================================


-- ============================================================
-- 1) events :: 이벤트 소싱 진실의 원천 (append-only)
-- ============================================================
-- 도메인 ChatEvent (LifecycleEvent / UserEvent / MessageEvent) 가 모두 저장.
--
-- 인덱스 설계
--   PK (event_id)                       : 단일 이벤트 조회
--   uk_events_session_client            : (멱등성) 동일 클라 재전송 INSERT 차단. event_type 포함 — 한 명령이 같은 clientEventId 로 여러 type(LIFECYCLE+USER) 발행 가능
--   uk_events_session_seq               : (순서) 동일 (session, seq) 충돌 차단 + 정렬/catch-up
--   idx_events_session_created          : (시점 복원) WHERE session_id=? AND created_at <=?
CREATE TABLE IF NOT EXISTS events (
    event_id        CHAR(36)    NOT NULL COMMENT 'UUID v7. PK. 도메인이 발급',
    session_id      CHAR(36)    NOT NULL COMMENT '세션 식별자',
    client_event_id CHAR(36)    NOT NULL COMMENT '클라이언트 멱등성 키. 재전송 시 동일',
    seq             BIGINT      NOT NULL COMMENT '서버 발급 시퀀스 (Redis INCR per session)',
    event_type      VARCHAR(20) NOT NULL COMMENT 'LIFECYCLE | USER | MESSAGE',
    payload         JSON        NOT NULL COMMENT 'type-specific 페이로드 (직렬화된 도메인 이벤트)',
    created_at      DATETIME(6) NOT NULL COMMENT '서버 수신 시각',

    PRIMARY KEY (event_id),
    UNIQUE KEY uk_events_session_client (session_id, client_event_id, event_type),
    UNIQUE KEY uk_events_session_seq    (session_id, seq),
    KEY        idx_events_session_created (session_id, created_at)
)
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci
COMMENT = '이벤트 소싱 append-only 저장소';


-- ============================================================
-- 2) sessions :: 세션 read model (Projection)
-- ============================================================
-- LifecycleEvent 를 폴드해서 만들어지는 read model.
-- ENDED 는 final (재개 불가) — 애플리케이션 레이어에서 보장.
--
-- 인덱스 설계
--   PK (session_id)                : 단건 조회
--   idx_sessions_status            : status 필터 목록 조회 (ACTIVE / SUSPENDED / ENDED)
--   idx_sessions_started_at        : 기간 필터 목록 조회
CREATE TABLE IF NOT EXISTS sessions (
    session_id   CHAR(36)    NOT NULL COMMENT '세션 식별자',
    status       VARCHAR(20) NOT NULL COMMENT 'ACTIVE | SUSPENDED | ENDED',
    started_at   DATETIME(6) NOT NULL COMMENT '최초 생성 시각',
    ended_at     DATETIME(6) NULL     COMMENT 'ENDED 상태 진입 시각',
    created_at   DATETIME(6) NOT NULL COMMENT 'row 생성 시각 (BaseEntity / JPA Auditing)',
    updated_at   DATETIME(6) NOT NULL COMMENT 'row 갱신 시각 (BaseEntity / JPA Auditing)',

    PRIMARY KEY (session_id),
    KEY idx_sessions_status     (status),
    KEY idx_sessions_started_at (started_at)
)
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci
COMMENT = '세션 lifecycle 프로젝션';


-- ============================================================
-- 3) session_users :: 세션 참여자 read model (Projection)
-- ============================================================
-- UserEvent (JOINED / LEFT) 를 폴드. is_active 로 현재 활성 여부 표현.
-- 같은 user 가 leave 후 다시 join 하면 기존 row 를 rejoin (재활성화).
--
-- 인덱스 설계
--   uk_session_user                : 동일 (session, user) 중복 row 방지 (rejoin 시 기존 row 재사용)
--   idx_session_user_session_id    : 세션 참여자 목록 조회
--   idx_session_user_user_id       : 사용자의 활성 세션 조회 (WebSocket 재연결 시)
--   idx_session_user_active        : is_active 필터 가속
CREATE TABLE IF NOT EXISTS session_users (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    session_id  CHAR(36)    NOT NULL COMMENT '세션 식별자',
    user_id     CHAR(36)    NOT NULL COMMENT '사용자 식별자',
    role        VARCHAR(20) NOT NULL COMMENT 'MEMBER 등 (확장 대비)',
    is_active   BOOLEAN     NOT NULL COMMENT '현재 세션에 활성 참여 중인지',
    joined_at   DATETIME(6) NOT NULL COMMENT '(재)참여 시각',
    left_at     DATETIME(6) NULL     COMMENT '마지막 퇴장 시각',
    created_at  DATETIME(6) NOT NULL COMMENT 'row 생성 시각 (BaseEntity / JPA Auditing)',
    updated_at  DATETIME(6) NOT NULL COMMENT 'row 갱신 시각 (BaseEntity / JPA Auditing)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_session_user (session_id, user_id),
    KEY idx_session_user_session_id (session_id),
    KEY idx_session_user_user_id    (user_id),
    KEY idx_session_user_active     (is_active)
)
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci
COMMENT = '세션 참여자 프로젝션';


-- ============================================================
-- 4) messages :: 메시지 read model (Projection)
-- ============================================================
-- MessageEvent (SENT / EDITED / DELETED) 를 폴드. created_at/updated_at 은 projection 반영/갱신 시각 (JPA Auditing). 메시지 작성 시각 자체는 events 테이블에 보존.
--
-- 인덱스 설계
--   PK (message_id)                : 단건 조회 / EDIT-DELETE 핫패스
--   idx_message_session_seq        : 세션 내 메시지 목록 정렬 / 페이징
--   idx_message_sender             : 사용자가 보낸 메시지 조회
CREATE TABLE IF NOT EXISTS messages (
    message_id  CHAR(36)    NOT NULL COMMENT '메시지 식별자 (UUID v7)',
    session_id  CHAR(36)    NOT NULL COMMENT '세션 식별자',
    sender_id   CHAR(36)    NOT NULL COMMENT '발신자 식별자',
    content     TEXT        NULL     COMMENT '메시지 본문 (DELETED 후에도 보존하나 status 로 가림)',
    status      VARCHAR(20) NOT NULL COMMENT 'SENT | EDITED | DELETED',
    seq         BIGINT      NOT NULL COMMENT '세션 내 순서 (이벤트의 seq 복사)',
    created_at  DATETIME(6) NOT NULL COMMENT 'projection 반영 시각 (BaseEntity / JPA Auditing)',
    updated_at  DATETIME(6) NOT NULL COMMENT 'projection 갱신 시각 (BaseEntity / JPA Auditing)',

    PRIMARY KEY (message_id),
    KEY idx_message_session_seq (session_id, seq),
    KEY idx_message_sender      (sender_id)
)
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci
COMMENT = '메시지 프로젝션';


-- ============================================================
-- 5) users :: 회원 (이벤트 소싱 밖, 일반 CRUD)
-- ============================================================
-- 인증/인가 비목표 (과제 가정). id 는 서버 발급 PK, user_id 는 클라가 지정하는 식별자(로그인 아이디).
-- last_seen_at 은 presence (마지막 접속 시각) — WebSocket close / 메시지 송신 시 갱신.
--
-- 인덱스 설계
--   PK (id)                         : 서버 발급 내부 식별자
--   uk_users_user_id                : 클라 지정 userId 중복 차단 (WebSocket ?userId=, senderId 가 참조). username 은 중복 허용
CREATE TABLE IF NOT EXISTS users (
    id            CHAR(36)    NOT NULL COMMENT '서버 발급 PK (UUID v7)',
    user_id       VARCHAR(50) NOT NULL COMMENT '클라 지정 식별자 (로그인 아이디)',
    username      VARCHAR(50) NOT NULL COMMENT '표시 이름 (중복 허용)',
    status        VARCHAR(50) NULL     COMMENT '확장용 (현재 미사용)',
    last_seen_at  DATETIME(6) NULL     COMMENT 'presence: 마지막 접속 시각',
    created_at    DATETIME(6) NOT NULL COMMENT 'row 생성 시각 (BaseEntity / JPA Auditing)',
    updated_at    DATETIME(6) NOT NULL COMMENT 'row 갱신 시각 (BaseEntity / JPA Auditing)',

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_user_id (user_id)
)
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci
COMMENT = '회원';

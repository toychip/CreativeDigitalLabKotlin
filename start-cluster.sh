#!/bin/bash

# 분산 채팅 시스템 시작 스크립트 (chat-cdl 루트에서 실행)
COMPOSE="docker-compose -f docker/docker-compose.yml"

echo "Starting Distributed Chat System..."

# 환경 체크
if ! command -v docker &> /dev/null; then
    echo "Docker가 설치되지 않았습니다."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "Docker Compose가 설치되지 않았습니다."
    exit 1
fi

# 기존 컨테이너 정리 (선택적)
read -p "기존 컨테이너를 정리하시겠습니까? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "기존 컨테이너 정리 중..."
    $COMPOSE down --volumes --remove-orphans
fi

# 데이터베이스 / Redis 먼저 기동
echo "MySQL 및 Redis 시작 중..."
$COMPOSE up -d mysql redis

echo "MySQL 준비 대기 중..."
until $COMPOSE exec -T mysql mysqladmin ping -h localhost -u root -p12345678 --silent &> /dev/null; do
    sleep 2
done

echo "Redis 준비 대기 중..."
until $COMPOSE exec -T redis redis-cli ping &> /dev/null; do
    sleep 2
done

# 애플리케이션 빌드 및 기동
echo "채팅 애플리케이션 빌드 중..."
$COMPOSE build chat-app-1 chat-app-2 chat-app-3

echo "채팅 애플리케이션 인스턴스 시작 중..."
$COMPOSE up -d chat-app-1 chat-app-2 chat-app-3

# 로드 밸런서 기동
echo "Nginx 로드 밸런서 시작 중..."
$COMPOSE up -d nginx

# 상태 확인
echo "서비스 상태 확인 중..."
$COMPOSE ps

echo ""
echo "분산 채팅 시스템이 시작되었습니다!"
echo ""
echo "접속:"
echo "  REST:      http://localhost/..."
echo "  WebSocket: ws://localhost/ws/chat?userId=<id>"
echo "  Actuator:  http://localhost/actuator/health  (metrics: /actuator/metrics)"
echo ""
echo "모니터링:"
echo "  $COMPOSE logs -f chat-app-1 chat-app-2 chat-app-3"
echo "  $COMPOSE logs -f nginx"
echo ""
echo "참고: 채팅 애플리케이션을 스케일아웃하고 ddl-auto 로 진행하여(첫 초기화용)"
echo "      처음에는 인덱스 생성 중복 오류가 발생할 수 있으나,"
echo "      스케일아웃으로 인한 것이기 때문에 무시하셔도 됩니다."
echo ""
echo "종료: $COMPOSE down"

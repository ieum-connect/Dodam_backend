# 복약·일정 Web Push 연결

## 설정
기본값은 `PUSH_ENABLED=false`이며 기존 앱 내 알림은 유지된다.
푸시를 켤 때 `.env.example`의 FIREBASE_* 항목을 같은 Firebase 프로젝트 값으로 설정한다.
`FIREBASE_CREDENTIALS`는 서버에서 읽을 수 있는 서비스 계정 JSON 경로다. 키 파일은 저장소에 넣지 않고 읽기 전용으로 마운트한다.
웹 설정 API에는 공개 웹 설정만 반환하며 서비스 계정 키는 반환하지 않는다.
프론트엔드의 PWA Service Worker와 함께 적용하고 HTTPS에서 알림 권한을 허용한다(localhost 예외).

## API와 발송
- GET /api/push/config: 웹 설정
- POST /api/push/devices: 로그인 사용자 기기 등록. JSON {"token":"FCM token"}
- DELETE /api/push/devices: 현재 사용자 기기 해제. 같은 본문
- 기존 복약·일정 앱 내 알림 생성 시 기기별 발송 대기 항목을 생성한다.
- 발송 직전 사용자/가족 권한, 기기 활성 여부, 일정 취소·완료·시간 변경을 다시 검사한다.
- 최대 5회 실패 처리, 15분 만료, UNREGISTERED 토큰 비활성화를 적용한다.
- DB 유일 제약과 잠금으로 일반적인 중복 작업을 막는다. 외부 발송 직후 프로세스가 종료되는 경우까지 정확히 한 번 전송을 보장하지는 않는다.
- 잠금 화면에는 아이 이름·약 이름 대신 일반 안내를 표시한다.

## DB와 검증
V202611101600__create_push_delivery_outbox.sql: push_devices, push_deliveries 신규 테이블.
기존 develop에 같은 번호 없음. 로컬 PostgreSQL에 적용해 실행했다.
자동 테스트 388개 통과(실패/오류 0). 테스트는 실제 Firebase 대신 모의 발송을 사용한다.
2026-11-10: 로컬 Chrome에서 복약·일정 2건 모두 FCM SENT, 사용자가 알림 수신 및 클릭 후 목록 진입 확인.
휴대폰 Android/iOS 설치 PWA 및 운영 서버 검증은 아직 수행하지 않았다.
유료 AI 호출 없음. 배포 시 서비스 계정 마운트와 환경 변수 설정이 별도로 필요하다.

## 다중 서버 확장 시 제약
현재 deliverOne은 DB 행 잠금을 잡은 트랜잭션 안에서 Firebase 외부 전송을 수행한다(연결 5초/읽기 10초 제한).
다른 인스턴스는 같은 행의 잠금을 기다릴 수 있으므로 현재 단일 서버 운영을 전제로 한다.
다중 서버로 확장하기 전 SKIP LOCKED 또는 상태 선점·임대 만료를 적용하고, 선점 트랜잭션과 외부 발송을 분리한 뒤 장애 회복/중복 발송을 검증해야 한다.

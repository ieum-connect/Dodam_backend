# 도담(Dodam) – 우리 아이에게 필요한 복지를 찾아주는 가족 돌봄 플랫폼

<div align="center">

<!-- 서비스 대표 이미지가 준비되면 아래 src를 교체해 주세요. -->
<img width="960" alt="도담 서비스 대표 이미지" src="[대표 이미지 URL을 입력해 주세요]">

</div>

---

## 👩‍👧‍👦 1. 팀 소개

| 이름 | 역할 | 담당 영역 |
| --- | --- | --- |
| [이름을 입력해 주세요] | [역할] | [담당 영역] |
| [이름을 입력해 주세요] | [역할] | [담당 영역] |
| [이름을 입력해 주세요] | [역할] | [담당 영역] |

> 협업 규칙: 기능 단위 브랜치 → PR 리뷰 → `main` 머지. 모듈(도메인) 단위로 담당을 나누어 작업했고, 공통 설정(`global`)과 DB 마이그레이션은 변경 전 공유 후 반영했습니다.

---

## ⚒️ 2. 서비스 소개

### 2-1. 📃 프로젝트 기획

> **도담은 아이의 건강·성장 정보와 가족의 돌봄 상황을 바탕으로, 우리 가족에게 필요한 복지 프로그램을 맞춤 추천하는 가족 건강 관리 서비스입니다.**
>
> 흩어진 건강 기록과 복지 정보를 연결해 보호자가 받을 수 있는 지원을 놓치지 않도록 하고, 자격 조건·신청 방법·신청 시기를 이해하기 쉽게 안내합니다.

- **한 줄 정의**: 아이별 건강·방문·문서 기록을 모으고, 그 기록을 근거로 복지 프로그램을 추천하며, 어려운 의료 문서를 AI가 보호자의 언어로 풀어 주는 서비스.
- **대상 사용자**: 영유아·아동을 키우는 보호자(부모, 조부모 등 공동 양육자), 맞벌이·한부모·다자녀 가정.
- **핵심 사용 흐름**: 로그인 → 아이 등록 → 병원 방문·건강 기록 작성 → 진료 문서 업로드 → AI 해설·근거 확인 → 우리 아이에게 맞는 복지 프로그램 추천 확인 → 신청 준비(조건·서류·기간).
- **범위(MVP)**: 아이 프로필, 보호자 연결, 병원 방문 기록, 건강 기록, 의료 문서 업로드·AI 해설, AI 상담 채팅, 알림, 복지 추천.
- **다음 단계**: 모바일 앱 전환, 복지 데이터 소스 확장, 추천 근거 품질 평가 자동화.

🔗 프로젝트 기획서: **[기획서 링크를 입력해 주세요]**

### 2-2. ❗ 문제 인식

<details><summary>2-2-1. 필요한 복지 프로그램을 찾기 어렵습니다</summary>

- 복지 제도가 여러 기관과 웹사이트에 흩어져 있습니다. (중앙부처, 지자체, 보건소, 어린이집·유치원 공고 등)
- 지원 대상, 소득 기준, 지역 조건, 신청 기간을 직접 비교해야 합니다. 같은 이름의 사업도 지역마다 기준이 달라 "우리 집이 대상인지"를 판단하기 어렵습니다.
- 보호자는 검색과 서류 확인에 많은 시간을 씁니다. 용어가 행정 중심이라 무엇을 준비해야 하는지 바로 알기 어렵습니다.
- 결과적으로 **자격이 되는데도 신청하지 못하는 누락**이 발생합니다.

</details>

<details><summary>2-2-2. 아이의 건강·돌봄 기록이 분산되어 있습니다</summary>

- 예방접종, 진료, 성장 기록과 문서가 여러 곳에 나뉘어 있습니다. (병원 앱, 종이 수첩, 사진첩, 메신저)
- 복지 상담이나 신청에 필요한 정보를 다시 정리해야 합니다. 진단명, 방문 일자, 검사 결과를 매번 찾아 옮겨 적습니다.
- 보호자마다 정보가 달라 누락과 중복이 생길 수 있습니다. 한 사람만 알고 있는 기록은 다른 보호자가 병원에 갔을 때 전달되지 않습니다.
- 진료 기록·검사지의 **의료 용어가 어려워** 내용을 이해하지 못한 채 보관만 하게 됩니다.

</details>

<details><summary>2-2-3. 바쁜 보호자는 신청 시기를 놓치기 쉽습니다</summary>

맞벌이·한부모·다자녀 가정은 필요한 지원을 알고도 신청 기간과 준비 서류를 놓치기 쉽습니다. 도담은 가족 상황을 기준으로 우선순위를 정리하고 다음 행동을 안내합니다.

- 신청 기간이 짧거나 선착순인 사업이 많아 "알고 있었는데 지났다"가 반복됩니다.
- 아이가 둘 이상이면 아이마다 해당 사업과 시기가 달라 관리 난도가 올라갑니다.
- 도담은 추천 결과를 **지금 할 일 / 기간이 임박한 일 / 참고할 일**로 나누어 보여 주고, 알림으로 다시 한 번 상기시킵니다.

</details>

### 2-3. 🎯 도담의 해결 방향

<details><summary>2-3-1. 복지 프로그램을 맞춤 추천합니다</summary>

- 아이의 나이·건강 상태·지역과 가족 조건을 바탕으로 추천합니다.
- 대상 조건, 지원 내용, 신청 기간, 준비 서류를 함께 보여줍니다.
- 추천 결과를 우선순위로 정리해 바로 다음 행동을 선택할 수 있게 합니다.
- 추천에 쓰인 근거(어떤 기록·어떤 조건 때문에 추천되었는지)를 함께 노출해, 보호자가 스스로 판단할 수 있게 합니다.
- 해당되지 않는 사업은 **왜 해당되지 않는지**(지역 불일치, 연령 초과 등)를 함께 알려 불필요한 시도를 줄입니다.

</details>

<details><summary>2-3-2. 건강 기록과 문서를 연결합니다</summary>

- 아이별 예방접종·방문·성장 기록을 관리합니다. 병원 방문(`visits`)을 중심으로 그날의 기록과 문서가 함께 묶입니다.
- 업로드한 문서를 상담과 추천의 근거로 활용합니다. PDF·이미지를 페이지 단위로 분리하고 텍스트를 추출해 저장합니다.
- 보호자 관계를 연결해 가족이 같은 아이의 정보를 함께 확인합니다. 초대 → 수락/거절 → 연결 해제까지 상태를 관리합니다.
- 접근 정책(`AccessPolicy`)으로 **연결된 보호자만** 해당 아이의 기록·문서를 조회할 수 있게 제한합니다.
- 개인 계정과 보호자 계정을 전환(`/api/accounts/switch`)해 역할에 맞는 화면을 사용할 수 있습니다.

</details>

<details><summary>2-3-3. AI 도담이 이해하기 쉽게 설명합니다</summary>

등록된 건강 문서와 서비스 지식을 바탕으로 질문에 답하고, 근거가 부족하거나 의료기관 확인이 필요한 내용은 별도로 안내합니다.

- **문서 해설**: 업로드한 진료 기록·검사지를 섹션(요약/주요 수치/다음 할 일 등)으로 나누어 쉬운 말로 정리합니다.
- **근거 인용**: 해설의 각 항목이 문서의 어느 페이지에서 나왔는지 인용(citation)으로 연결해, 사용자가 원문을 바로 확인할 수 있습니다.
- **AI 상담 채팅**: 세션 단위로 대화를 저장하고, 답변은 등록된 문서와 서비스 지식 범위 안에서 생성합니다.
- **안전 장치**: 근거가 부족하면 단정하지 않고 "확인 필요"로 표시하며, 진단·응급 판단은 하지 않고 의료기관 상담을 안내합니다. (`prompts/safety`)
- **실패 처리**: 분석 실패 유형(파일 손상, 페이지 초과, 모델 오류 등)을 구분해 사용자에게 다음 행동을 안내합니다.

</details>

### 2-4. 🌱 기대 효과

- 받을 수 있는 복지 프로그램을 빠르게 발견할 수 있습니다. — 여러 사이트를 돌지 않고 한 화면에서 확인합니다.
- 신청 조건과 준비 서류를 미리 확인해 누락을 줄일 수 있습니다. — 기간 임박 사업을 먼저 보여 줍니다.
- 건강 기록과 복지 정보를 한곳에서 관리해 가족 돌봄의 연속성이 높아집니다. — 보호자가 바뀌어도 같은 기록을 봅니다.
- 복지 탐색에 쓰던 시간을 줄이고 아이를 돌보는 일에 집중할 수 있습니다.
- 어려운 의료 문서를 이해한 상태로 다음 진료·상담에 들어갈 수 있습니다.
- 기록이 쌓일수록 추천과 상담의 정확도가 올라가 서비스 사용 가치가 누적됩니다.

### 2-5. 🌼 핵심 차별점

| 구분 | 기존 방식 | 도담 |
| --- | --- | --- |
| 복지 탐색 | 여러 기관·사이트를 직접 검색 | 아이·가족 상황 기반 맞춤 추천 |
| 정보 이해 | 조건과 서류를 따로 확인 | 추천 결과에 조건·신청 방법을 함께 제공 |
| 건강 기록 | 앱·문서에 분산 | 아이별 기록과 문서를 통합 |
| 가족 돌봄 | 보호자별 정보가 분리 | 보호자 관계 연결로 공동 확인 |
| 상담 | 일반적인 검색 결과 | 문서 근거를 활용한 AI 도담 상담 |
| 문서 해석 | 의료 용어 그대로 보관 | 페이지 인용과 함께 쉬운 말로 해설 |
| 신뢰성 | 출처 불명의 요약 | 근거 부족 시 "확인 필요"로 표시 |

---

## 🚀 3. 기술 스택

<div align="center">
<img src="https://img.shields.io/badge/Java_17-007396?style=for-the-badge&logo=java&logoColor=white" />
<img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
<img src="https://img.shields.io/badge/Spring_Data_JPA-59666C?style=for-the-badge" />
<img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" />
<br>
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
<img src="https://img.shields.io/badge/pgvector-336791?style=for-the-badge" />
<img src="https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white" />
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
</div>

### 3-1. 구성 요소

| 구분 | 기술 | 역할 |
| --- | --- | --- |
| 백엔드 | Java 17, Spring Boot, Spring Data JPA, Spring Security(JWT·OAuth2 Client), Flyway, PDFBox | 도메인 API, 인증·인가, 접근 정책, 스키마 마이그레이션, 문서 저장 |
| AI 백엔드 | Python 3.12, FastAPI, Pydantic v2, SQLAlchemy 2, psycopg3, PyMuPDF, Pillow, httpx, OpenAI 호환 클라이언트 | 문서 페이지 분리·렌더링, 추출·해설 생성, 결과 콜백 |
| 데이터베이스 | PostgreSQL 16 (`pgvector/pgvector:pg16`), pgvector | 관계형 데이터 + 임베딩 기반 검색 |
| 프론트엔드 | React, Vite, TypeScript, lucide-react | 모바일 퍼스트 웹앱(앱 전환 전 단계) |
| 인프라 | Docker Compose | 로컬 PostgreSQL 실행, 데모 데이터 적재 |
| 품질 | JUnit·Spring Security Test, pytest, ruff, H2(테스트용) | 단위·통합 테스트, 린트 |

### 3-2. 시스템 구조

```
[ React + Vite 웹앱 ]
          │  REST (JWT)
          ▼
[ Spring Boot 백엔드 ]  ──► PostgreSQL + pgvector
          │  내부 API (AI_INTERNAL_TOKEN)
          ▼
[ FastAPI AI 백엔드 ]  ──► LLM / VLM (OpenAI 호환)
          │  결과 콜백
          └──────────────► Spring Boot 백엔드
```

- 프론트엔드는 Spring Boot의 REST API만 호출하고, AI 백엔드를 직접 호출하지 않습니다.
- Spring Boot는 문서 업로드 이벤트를 만들고 AI 백엔드에 분석을 요청하며, 결과는 콜백으로 받아 저장합니다.
- 내부 호출은 공유 토큰(`AI_INTERNAL_TOKEN`)으로 보호합니다.

### 3-3. 백엔드 모듈 구조

도메인별로 `api / command / query / domain / infrastructure`를 나눈 모듈 구조입니다. (`com.ieum.dodam`)

| 모듈 | 설명 |
| --- | --- |
| `user` | 회원, 인증, 역할 설정, 계정 전환, 내 정보 |
| `patient` | 아이(아동) 프로필, 보호자 목록, 초대 참여 |
| `care_relation` | 보호자 연결 요청·수락·거절·해제 |
| `visit` | 병원 방문 기록 CRUD, 방문에 문서 연결 |
| `health_record` | 건강 기록 CRUD와 접근 정책 |
| `medical_document` | 문서 업로드·삭제·조회, 분석 상태, AI 해설/인용/섹션 조회 |
| `ai_chat` | AI 상담 세션과 메시지 |
| `ai_result` | AI 결과 문서 조회 |
| `action_item` | 추천·해설에서 파생된 할 일과 승인 상태 |
| `notification` | 시스템 알림, 읽음·전체 읽음 처리 |
| `schedule`, `timeline`, `voice_recording` | 일정·타임라인·음성 기록(확장 영역) |
| `global` | 인증 필터, 공통 설정, 예외 처리, 공통 타입·상태 |

### 3-4. 주요 API

| 영역 | 엔드포인트 |
| --- | --- |
| 인증·계정 | `GET /api/auth/config`, `GET /api/auth/needs-role`, `POST /api/role`, `PUT /api/me/role`, `GET /api/me`, `DELETE /api/me`, `GET /api/accounts`, `POST /api/accounts/switch`, `POST /api/demo/login` |
| 아이 | `GET·POST /api/children`, `PUT /api/children/{id}`, `GET /api/children/{id}/parents`, `POST /api/children/join` |
| 보호자 연결 | `GET·POST /api/care-relations`, `GET /api/care-relations/{id}`, `POST /api/care-relations/{id}/accept`, `POST /api/care-relations/{id}/reject`, `DELETE /api/care-relations/{id}/request`, `DELETE /api/care-relations/{id}` |
| 병원 방문 | `GET·POST /api/visits`, `GET·PUT·DELETE /api/visits/{visitId}` |
| 건강 기록 | `GET·POST /api/health-records`, `GET·PUT·DELETE /api/health-records/{recordId}` |
| 문서 | `POST /api/visits/{visitId}/documents` (multipart), `GET /api/documents`, `GET /api/documents/{documentId}`, `DELETE /api/documents/{documentId}` |
| AI 문서 해설 | `GET /api/ai-documents`, `GET /api/ai-documents/{documentId}`, `.../sections`, `.../facts`, `.../citations`, `.../pages/{pageNo}`, `.../explanation-status` |
| AI 상담 | `POST·GET /api/ai-chat-sessions/{sessionId}/messages` |
| AI 결과 문서 | `GET /api/ai-result-documents/{resultDocumentId}` |
| 알림 | `GET /api/notifications`, `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all` |

### 3-5. AI 백엔드 파이프라인

- `pipelines/documents` – PDF·이미지를 페이지로 분리하고(`page_splitter`) 지정 DPI로 렌더링합니다. (기본 150 DPI, 최대 30페이지, 최대 변 2048px)
- `pipelines/rag` – 문서 청크와 임베딩을 이용해 질문에 관련된 근거를 검색합니다.
- `pipelines/summaries` – 진료 전 요약과 문서 해설을 생성합니다.
- `pipelines/comparisons` – 기록 간 변화 비교.
- `pipelines/speech` – 음성 기록 전사(확장 영역).
- `prompts/` – `document_extraction`, `document_explanation`, `chat_answer`, `previsit_summary`, `safety` 프롬프트를 파일로 분리해 버전 관리합니다.
- `evals/` – 프롬프트 변경 시 품질을 확인하는 평가 스크립트.

### 3-6. 데이터 모델 요약

Flyway 마이그레이션(`src/main/resources/db/migration`)으로 관리하는 주요 테이블입니다.

- **사용자·관계**: `users`, `caregiver_links`, `patients`, `consents`
- **기록**: `visits`, `visit_participants`, `health_records`, `schedules`, `action_items`
- **문서**: `documents`, `document_pages`, `document_chunks`, `document_analyses`, `extracted_items`
- **AI**: `ai_runs`, `ai_explanations`, `explanation_sections`, `explanation_items`, `explanation_citations`, `ai_claims`, `claim_evidence`, `embeddings`, `retrieval_results`
- **상담·알림**: `chat_sessions`, `chat_messages`, `notifications`, `system_notifications`
- **감사**: `audit_logs`

---

## 🗂️ 4. 설계·산출물 문서

> 실제 문서는 사용자가 추가할 수 있도록 자리만 마련했습니다.

- ### 📝 프로젝트 기획서
  **[기획서 링크를 입력해 주세요]**
- ### 📚 요구사항 명세서
  **[요구사항 명세서 링크를 입력해 주세요]**
- ### 📖 API 명세서
  **[API 명세서 링크를 입력해 주세요]**
  <details><summary>API 명세서 이미지</summary>
  `<!-- API 명세서 이미지를 여기에 추가해 주세요. -->`
  </details>
- ### 🗺️ ERD
  **[ERD 링크를 입력해 주세요]**
  <details><summary>ERD 이미지</summary>

  ![도담 ERD](docs/dodam_erd.svg)

  `<!-- 외부 ERD 이미지를 사용하려면 위 경로를 교체해 주세요. -->`
  </details>
- ### 🖼️ 화면 설계서
  **[Figma 또는 화면 설계서 링크를 입력해 주세요]**
- ### 🧪 테스트 결과서
  **[백엔드/통합 테스트 결과서 링크를 입력해 주세요]**
- ### 📄 기능 문서
  - 아동 서비스 상세: [`docs/CHILD_SERVICE.md`](docs/CHILD_SERVICE.md)

---

## ▶️ 5. 실행 방법

### 5-1. 사전 준비

| 항목 | 버전 |
| --- | --- |
| JDK | 17 |
| Python | 3.12 이상 |
| Node.js | 20 이상 권장 |
| Docker Desktop | 최신 |

### 5-2. 백엔드

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

- PostgreSQL은 호스트 **5433** 포트로 노출되고, 계정은 `dodam / dodam_local`, DB는 `dodam` 입니다.
- 애플리케이션 시작 시 Flyway가 마이그레이션을 적용합니다.
- 환경 변수는 `.env.example`을 복사해 `.env`로 만든 뒤 채워 주세요.

### 5-3. AI 백엔드

```powershell
cd ..\Dodam_AI_backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e ".[dev]"
uvicorn app.main:app --reload --port 8001
```

필요한 환경 변수(`.env`):

| 변수 | 설명 |
| --- | --- |
| `DATABASE_URL` | PostgreSQL 접속 문자열 |
| `AI_INTERNAL_TOKEN` | Spring ↔ AI 백엔드 내부 호출 토큰 |
| `STORAGE_LOCAL_ROOT` | 업로드 문서 저장 루트 경로 |
| `LLM_API_KEY`, `LLM_BASE_URL`, `LLM_MODEL` | 해설·상담 생성용 LLM 설정 |
| `OPENAI_API_KEY`, `OPENAI_VLM_MODEL` | 문서 이미지 분석(VLM) 설정 |

헬스 체크: `GET http://localhost:8001/health`

### 5-4. 프론트엔드

```powershell
cd ..\Dodam_frontend
npm install
npm run dev
```

프로덕션 빌드:

```powershell
npm run build
```

### 5-5. 데모 데이터

```powershell
docker cp .\dummydata dodam_backend-postgres-1:/tmp/dummydata
docker exec -e PGCLIENTENCODING=UTF8 dodam_backend-postgres-1 `
  psql -U dodam -d dodam -f /tmp/dummydata/all-dodam-demo.sql
```

적재 후 `POST /api/demo/login` 으로 데모 계정에 로그인해 화면을 바로 확인할 수 있습니다.

### 5-6. 테스트

```powershell
# 백엔드
.\mvnw.cmd test

# AI 백엔드
pytest
ruff check .
```

---

## 🔁 6. 형상 관리

- 브랜치 전략: **[브랜치 전략 이미지 또는 문서 링크를 입력해 주세요]**
  - `main`: 배포 가능 상태 유지
  - `feature/<도메인>-<작업>`: 기능 단위 작업 브랜치
  - `fix/<이슈번호>`: 버그 수정
- 커밋 메시지: `<타입>: <요약>` (`feat`, `fix`, `refactor`, `docs`, `test`, `chore`)
- PR 규칙: 변경 목적·테스트 결과·화면 또는 응답 예시를 포함하고, 리뷰 1인 승인 후 머지
- DB 변경: 반드시 Flyway 마이그레이션 파일(`V<타임스탬프>__<설명>.sql`)로 추가하고 기존 파일은 수정하지 않음
- 이슈 관리: **[GitHub Issues / 프로젝트 링크를 입력해 주세요]**
- 작업 트래킹: **[작업 보드 링크를 입력해 주세요]**

---

## ⚠️ 7. 서비스 이용 시 참고

도담의 AI 안내는 건강·복지 정보 탐색을 돕기 위한 참고 정보입니다. 진단이나 응급 판단을 대신하지 않으며, 의료적 판단이 필요한 경우 의료기관과 상담해야 합니다.

- 복지 프로그램의 자격 조건과 신청 기간은 기관·지역·연도에 따라 달라질 수 있으므로, 최종 확인은 담당 기관 공고를 따라 주세요.
- 업로드한 의료 문서와 아이의 건강 기록은 민감 정보입니다. 연결된 보호자에게만 공개되며, 불필요한 문서는 삭제해 주세요.
- AI 해설은 문서에서 확인된 범위만 설명하고, 근거가 부족한 내용은 "확인 필요"로 표시합니다.

<div align="center">

<img src="./docs/assets/dodam_logo_effect.gif" width="100%" alt="도담 애니메이션 배너" />

# 도담 Backend

### 아이의 기록을 잇고, 가족의 돌봄을 이어가는 도담 💗

[Frontend](https://github.com/ieum-connect/Dodam_frontend) · [AI Backend](https://github.com/ieum-connect/Dodam_AI_backend)

</div>

---

## 1. 🛠️ 기술 스택

<div align="center">

### ⚙️ Backend · Security

![Java](https://img.shields.io/badge/Java_17-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![OAuth2](https://img.shields.io/badge/OAuth2-Kakao_Login-FFCD00?style=for-the-badge&logo=kakao&logoColor=191919)
![JWT](https://img.shields.io/badge/JWT-Cookie_Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

### 🗄️ Data · Storage

![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![pgvector](https://img.shields.io/badge/pgvector-Vector_Search-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_Private_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)

### 🔗 Integration · Quality

![Firebase](https://img.shields.io/badge/Firebase_Cloud_Messaging-FFCA28?style=for-the-badge&logo=firebase&logoColor=20232A)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![JUnit5](https://img.shields.io/badge/JUnit_5-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

</div>

<details>
<summary><b>🔎 기술을 어디에 사용하는지 보기</b></summary>

| 영역 | 기술 | 역할 |
|---|---|---|
| Application | Java 17 · Spring Boot 3.3 | REST API와 Domain Service 실행 |
| Security | Spring Security · OAuth2 · JJWT | Kakao Login, 권한 검사와 Cookie 인증 |
| Data | Spring Data JPA · PostgreSQL · pgvector | 서비스 데이터와 문서 Embedding 저장 |
| Migration | Flyway | Version 기반 Database Schema 관리 |
| Storage | Local Storage · AWS Private S3 | 의료 문서 원본과 변환 파일 보관 |
| Integration | Firebase Admin · PDFBox · HTTP Client | Push, PDF 검증과 외부 서비스 연동 |
| Quality | Maven · JUnit 5 · GitHub Actions | Build, 자동 Test와 Pull Request CI |

</details>

---

## 2. ❗ Pain Point와 해결 방향

도담 Backend가 해결하려는 문제를 **사용자가 겪는 불편 → 서버가 담당하는 해결 방식** 순서로 정리했습니다.

<details open>
<summary><b>🗂️ 아이의 건강·돌봄 기록이 여러 곳에 흩어져 있습니다</b></summary>

### Pain Point

- 병원 방문, 검사 결과지, 육아 메모, 성장 기록과 일정이 종이·사진·메신저에 나뉩니다.
- 재진이나 보호자 교대 때 필요한 기록을 다시 찾기 어렵고 중요한 정보가 빠질 수 있습니다.
- 같은 내용을 여러 번 입력하거나 서로 다른 버전으로 보관하게 됩니다.

### Backend의 해결 방식

- 모든 기록을 <code>아이(patient)</code> 기준으로 연결합니다.
- 방문, 건강 기록, 성장, 문서, 일정과 예방접종을 각각의 Domain으로 관리하고 Timeline으로 조합합니다.
- Validation, Idempotency, Transaction과 Lock으로 중복 저장과 일부 저장을 줄입니다.

</details>

<details>
<summary><b>👨‍👩‍👧 여러 보호자가 함께 돌볼 때 권한과 정보가 어긋납니다</b></summary>

### Pain Point

- 여러 보호자가 같은 아이를 돌보지만 각자 알고 있는 정보가 다를 수 있습니다.
- URL이나 ID만으로 기록을 조회할 수 있다면 민감한 건강 정보가 노출될 수 있습니다.
- 연결 상태가 명확하지 않으면 누가 접근 가능한지 알기 어렵습니다.

### Backend의 해결 방식

- Kakao OAuth2 로그인과 JWT Cookie로 사용자를 식별합니다.
- 아이 관련 API마다 본인 Profile 또는 <code>ACTIVE</code> 보호자 관계를 확인합니다.
- 보호자 연결을 요청 → 수락·거절 → 연결 해제의 상태 흐름으로 관리합니다.

</details>

<details>
<summary><b>📄 의료 문서는 어렵고, AI 답변은 근거가 없으면 믿기 어렵습니다</b></summary>

### Pain Point

- 검사 결과지와 처방전은 용어가 어려워 보호자가 이해하기 어렵습니다.
- 범용 AI에 직접 올리면 아이별 권한, 원문 보관과 답변 출처를 관리하기 어렵습니다.
- 분석 실패나 같은 파일 재전송으로 결과가 중복될 수 있습니다.

### Backend의 해결 방식

- 원본을 검증해 Local Storage 또는 비공개 S3에 저장하고 처리 상태를 관리합니다.
- Frontend 대신 내부 Token으로 AI Backend에 분석과 상담을 요청합니다.
- AI 설명·Citation·할 일 후보를 원본 문서 ID와 연결합니다.
- Content Hash, Idempotency Key, 분석 상태와 재시도로 중복과 실패를 제어합니다.

</details>

<details>
<summary><b>📅 복약·재진·예방접종 일정을 놓치기 쉽습니다</b></summary>

### Pain Point

- 문서 속 복약 시간과 다음 방문일을 다시 옮기는 과정에서 누락이 생깁니다.
- 반복 복약과 예방접종은 아이마다 시기가 달라 수동 관리가 번거롭습니다.
- AI가 잘못 읽은 내용을 바로 일정으로 등록하면 위험할 수 있습니다.

### Backend의 해결 방식

- AI 추출 일정은 바로 저장하지 않고 <code>승인 대기 후보</code>로 보관합니다.
- 보호자가 확인하고 승인한 항목만 실제 일정으로 생성합니다.
- 단일 일정, 반복 복약, 완료·취소와 변경 이력을 관리합니다.
- 예정 시간이 지나면 중복 없이 앱 알림을 만들고 선택적으로 Firebase Push를 전송합니다.

</details>

<details>
<summary><b>🎁 우리 가족에게 맞는 복지 제도를 찾고 판단하기 어렵습니다</b></summary>

### Pain Point

- 복지 정보가 여러 기관에 흩어져 있고 지역·연령·가구 조건이 서로 다릅니다.
- 많은 공고 중 신청 가능성이 있는 제도와 추천 이유를 판단하기 어렵습니다.

### Backend의 해결 방식

- 공공데이터를 복지 Catalog로 동기화하고 조건을 표준화합니다.
- 아이·가구 Profile을 규칙에 대입해 대상 여부와 추천 근거를 계산합니다.
- 추천 판정은 Spring이 담당하고 AI Backend는 확인된 근거를 쉽게 설명합니다.

</details>

---

## 3. ✨ 핵심 기능

| 기능 | 설명 |
|---|---|
| 🔐 인증·인가 | Kakao OAuth2, JWT HttpOnly Cookie, 역할과 아이 단위 권한 관리 |
| 👶 아이·보호자 | 아이 Profile과 보호자 초대·수락·거절·연결 해제 |
| 📝 건강·육아 기록 | 메모, 태그, 병원 방문과 성장 기록 관리 |
| 📅 일정·복약 | 직접 일정, AI 후보 승인, 반복 복약, 완료·취소와 알림 |
| 📄 의료 문서 | 파일 검증, 중복 방지, Local/S3 저장과 분석 상태 관리 |
| 🤖 AI 연동 | 문서 분석, 근거 조회, AI 상담과 복지 설명 요청 중계 |
| 💉 예방접종 | 아이별 접종 일정과 완료 여부 조회·변경 |
| 🎁 복지 추천 | 복지 Catalog, 조건 판정, 추천과 관심 목록 |
| 🔔 알림·Push | 시스템 알림, 일정 알림과 Firebase Web Push |

<details open>
<summary><b>🔐 인증과 아이 단위 접근 권한</b></summary>

1. Kakao OAuth2 또는 허용된 Demo Login으로 로그인합니다.
2. JWT를 JavaScript가 읽을 수 없는 HttpOnly Cookie로 발급합니다.
3. 역할과 계정을 선택한 뒤 아이 Profile 또는 보호자 관계를 확인합니다.
4. 기록·문서·일정 API마다 접근 권한을 다시 검증합니다.

> 로그인 성공만으로 모든 기록을 볼 수 없습니다. **인증된 사용자 + 해당 아이와의 관계**를 함께 확인합니다.

</details>

<details>
<summary><b>📝 기록·방문·성장과 Timeline</b></summary>

- **육아 메모**: 일상, 증상과 발달 관찰을 날짜·태그와 함께 저장합니다.
- **병원 방문**: 방문일, 병원, 진료 내용과 연결 문서를 한 진료 단위로 관리합니다.
- **성장 기록**: 키와 몸무게를 검증하고 시점별로 저장합니다.
- **Timeline**: 서로 다른 Domain의 기록을 아이와 시간 기준으로 묶습니다.

</details>

<details>
<summary><b>📅 일정·반복 복약과 보호자 승인</b></summary>

- AI 추출 결과는 승인 전까지 실제 일정이 아니며 거절하면 생성되지 않습니다.
- 반복 복약은 종료일·요일을 포함한 Series 규칙으로 관리합니다.
- 완료와 취소를 상태로 남겨 과거 기록을 보존합니다.
- 일정 변경 시 Version으로 다른 요청의 변경을 덮어쓰지 않습니다.

</details>

<details>
<summary><b>📄 문서 업로드·분석 상태와 원문 보호</b></summary>

- PDF·JPG·PNG의 크기, MIME Type, Page 수와 Image Pixel 수를 검증합니다.
- Idempotency Key와 Content Hash로 중복 생성을 방지합니다.
- 개발은 Local Folder, 배포는 Private S3를 선택할 수 있습니다.
- S3 원문은 공개하지 않고 접근 권한 확인 후 제공합니다.
- 업로드 → 분석 중 → 완료·실패 상태를 저장합니다.

</details>

<details>
<summary><b>🤖 AI 상담과 복지 추천</b></summary>

- Spring이 상담 Session·Message를 사용자와 아이에 연결합니다.
- AI에는 내부 Token과 권한이 확인된 문서 ID만 전달합니다.
- Citation을 문서·Page·근거와 연결해 원문을 확인할 수 있게 합니다.
- 복지 적합성은 Spring 규칙 Engine이 결정하고 AI는 근거를 쉽게 설명합니다.

</details>

---

### 🔄 핵심 서비스 Flow

<details open>
<summary><b>📄 문서 업로드 → AI 분석 → 근거 확인</b></summary>

~~~mermaid
sequenceDiagram
    actor User as 👤 보호자
    participant FE as 🎨 Frontend
    participant BE as ⚙️ Spring Backend
    participant FS as 📁 Local · S3
    participant AI as 🤖 AI Backend
    participant DB as 🗄️ PostgreSQL
    User->>FE: 검사 결과지·처방전 선택
    FE->>BE: multipart + Idempotency-Key
    BE->>BE: 로그인·아이 권한·파일 검사
    BE->>FS: 원본 저장
    BE->>DB: 문서와 UPLOADED 상태 저장
    BE->>AI: POST /internal/documents/{id}/analyze
    AI-->>BE: 202 Accepted
    AI->>FS: 원본 읽기
    AI->>AI: Page 변환 · 추출 · 설명 · Embedding
    AI->>DB: 근거·Citation·할 일 후보 저장
    loop 완료까지 상태 조회
        FE->>BE: 분석 상태 조회
        BE-->>FE: PROCESSING / READY / FAILED
    end
    FE-->>User: 쉬운 설명과 원문 근거 표시
~~~

</details>

<details>
<summary><b>💬 문서 기반 AI 상담</b></summary>

~~~mermaid
sequenceDiagram
    actor User as 👤 보호자
    participant FE as 🎨 Frontend
    participant BE as ⚙️ Spring Backend
    participant AI as 🤖 AI Backend
    participant DB as 🗄️ PostgreSQL · pgvector
    User->>FE: 문서에 관해 질문
    FE->>BE: Session Message 전송
    BE->>BE: 아이·문서 권한 검사
    BE->>DB: 질문 저장
    BE->>AI: 질문 + patientId + 허용 documentIds
    AI->>DB: 허용 범위 Vector Search
    AI->>AI: 안전 분류 · 답변 · Citation 검증
    AI-->>BE: 답변 + Citation
    BE->>DB: 답변과 근거 연결
    BE-->>FE: 저장된 답변 반환
~~~

</details>

<details>
<summary><b>📅 AI 추출 할 일 → 보호자 승인 → 일정 등록</b></summary>

~~~mermaid
flowchart LR
    DOC["📄 문서 분석"] --> CANDIDATE["🧾 Action Item<br/>PENDING"]
    CANDIDATE --> REVIEW{"👤 보호자 확인"}
    REVIEW -->|승인| SCHEDULE["📅 일정 생성"]
    REVIEW -->|거절| REJECTED["🚫 REJECTED 보존"]
    SCHEDULE --> NOTICE["🔔 앱 알림 · Push"]
    SCHEDULE --> DONE["✅ 완료"]
    SCHEDULE --> CANCEL["🗑️ 취소 상태 보존"]
~~~

AI가 읽은 날짜는 **후보**입니다. 보호자가 원문을 확인하고 승인하기 전에는 실제 일정이 되지 않습니다.

</details>

<details>
<summary><b>🎁 복지 동기화 → 조건 판정 → 추천 설명</b></summary>

~~~mermaid
flowchart LR
    GOV["🏛️ 공공 복지 데이터"] --> SYNC["Catalog 동기화"]
    SYNC --> NORMALIZE["지역 · 연령 · 소득 표준화"]
    PROFILE["👶 아이·가구 Profile"] --> RULE["⚙️ Rule Engine"]
    NORMALIZE --> RULE --> RESULT["추천 · 제외 · 판정 근거"]
    RESULT --> AI["🤖 쉬운 설명"]
    RESULT --> API["Spring API"]
    AI --> API --> FE["🎨 추천 화면"]
~~~

</details>

---

## 4. 🌱 기대 효과

<details open>
<summary><b>👨‍👩‍👧 사용자와 가족</b></summary>

- **기록 탐색 시간이 줄어듭니다.** 아이별 기록과 문서를 한 흐름으로 조회합니다.
- **보호자가 바뀌어도 돌봄이 이어집니다.** 연결된 가족이 같은 최신 정보를 확인합니다.
- **일정 누락을 줄입니다.** 승인된 일정만 알림으로 이어집니다.
- **AI 설명을 검증할 수 있습니다.** Page와 Citation으로 원문을 다시 확인합니다.
- **복지 탐색 부담을 줄입니다.** 가족 조건에 맞는 사업과 추천 이유를 제공합니다.

</details>

<details>
<summary><b>🧑‍💻 개발과 운영</b></summary>

- **보안 경계가 명확합니다.** Spring에서 인증·인가하고 AI는 내부 API로 분리합니다.
- **기능 확장이 쉽습니다.** Domain별 Package로 변경 영향 범위를 좁힙니다.
- **데이터 변경을 추적합니다.** Flyway Migration과 상태 이력을 사용합니다.
- **중복과 충돌을 줄입니다.** Idempotency, Transaction, Constraint와 Lock을 적용합니다.
- **환경 전환이 쉽습니다.** Local Storage와 Private S3를 선택할 수 있습니다.

</details>

---

## 5. 🧭 전체 시스템 아키텍처

> 도담은 **3개의 Code Repository와 공유 Infra**로 구성됩니다. Spring Backend가 인증·권한과 핵심 비즈니스 규칙을 책임지는 중앙 Hub입니다.

~~~mermaid
flowchart LR
    USER["👨‍👩‍👧 보호자<br/>Mobile · Desktop"] --> FE
    subgraph FE_REPO["🎨 Dodam_frontend · React PWA"]
        FE["화면 · 사용자 경험"] --> CLIENT["API Client<br/>Cookie 인증"]
        FE --> PWA["Service Worker<br/>Cache · Push"]
    end
    CLIENT -->|HTTPS · /api/*| BE
    subgraph BE_REPO["⚙️ Dodam_backend · Spring Boot"]
        BE["인증 · 권한 · API"] --> DOMAIN["아이 · 기록 · 일정 · 복지"]
        BE --> DOC["문서 · AI 작업 중계"]
        BE --> NOTICE["알림 · Push Outbox"]
    end
    DOC -->|Internal API<br/>X-Internal-Token| AI
    subgraph AI_REPO["🤖 Dodam_AI_backend · FastAPI"]
        AI["내부 AI API"] --> PIPE["문서 VLM Pipeline"]
        PIPE --> RAG["Chunk · Embedding · RAG"]
        RAG --> EXPLAIN["근거 기반 답변"]
        AI --> VACCINE["예방접종 일정 Engine"]
    end
    BE --> DB[("🗄️ PostgreSQL · pgvector")]
    AI --> DB
    DOC --> STORAGE[("📁 Shared Storage<br/>Local · Private S3")]
    PIPE --> STORAGE
    BE --> KAKAO["🟡 Kakao OAuth2"]
    BE --> GOV["🏛️ 공공 복지 API"]
    NOTICE --> FCM["🔔 Firebase Cloud Messaging"] --> PWA
    PIPE --> MODEL["☁️ VLM · LLM · Embedding API"]
    RAG --> MODEL
~~~

<details open>
<summary><b>🔎 저장소별 책임과 통신 원칙</b></summary>

| 구성 | 주요 책임 |
|---|---|
| 🎨 Frontend | 화면, 입력, 상태 표시, PWA와 Push 수신 |
| ⚙️ Spring Backend | 인증·권한, Domain 규칙, 검증, AI 중계와 알림 |
| 🤖 AI Backend | 문서 분석, RAG, Citation 답변, 복지 설명과 접종 계산 |
| 🗄️ PostgreSQL | 서비스 데이터와 Vector 저장 |
| 📁 Document Storage | 원본 의료 문서와 처리 Page 보관 |

1. Frontend는 Spring Backend만 호출합니다.
2. Spring이 사용자 인증과 아이 접근 권한을 먼저 확인합니다.
3. AI Backend는 내부 Token으로 보호합니다.
4. 두 Backend는 같은 PostgreSQL과 문서 Storage 설정을 사용합니다.
5. AI 결과도 Spring을 거쳐 사용자에게 전달됩니다.

</details>

---

## 6. 🧱 Code Architecture

~~~text
src/main/java/com/ieum/dodam
├── global/              # Security, JWT, OAuth2, 공통 설정·예외
├── user/                # 사용자 Profile, 역할, 계정 전환·탈퇴
├── patient/             # 아이 Profile과 보호자 목록
├── care_relation/       # 보호자 연결
├── health_record/       # 육아·건강 메모와 Tag
├── visit/ · growth/     # 병원 방문과 성장 기록
├── medical_document/    # 문서 Upload·Storage·AI 분석 요청
├── ai_result/ · ai_chat/ # AI 결과·근거와 상담
├── action_item/         # 문서에서 추출된 승인 대기 후보
├── schedule/            # 일정, 반복 복약과 Reminder
├── vaccination/        # 예방접종 일정과 완료 Check
├── welfare/             # 복지 Catalog·규칙·추천
├── notification/        # 앱 알림, Firebase Push와 Outbox
└── timeline/            # 기록을 시간순으로 조합

src/main/resources
├── application.yml
└── db/migration/        # Flyway Version Migration
~~~

<details open>
<summary><b>🧩 Domain 내부 구성</b></summary>

| 계층 | 역할 |
|---|---|
| api/controller | HTTP 요청과 인증 정보를 Application 계층에 전달 |
| api/dto · mapper | 요청 검증, 응답과 Domain 변환 |
| command/application | 생성·수정·삭제와 Transaction |
| query/application | 조회 Use Case와 읽기 조합 |
| domain/entity | 핵심 상태와 변경 규칙 |
| domain/repository | 저장·조회 Contract |
| infrastructure | 외부 HTTP, S3, Firebase와 JPA 구현 |
| error | 기능별 오류 Code와 예외 |

</details>

---

## 7. 📚 프로젝트 산출물

| 산출물 | 설명 | 링크 |
|---|---|---|
| 📋 요구사항 명세서 | 기능 요구사항, 범위와 검증 기준 | [요구사항 명세서 열기](https://docs.google.com/spreadsheets/d/1Pi5tciZ795vPqvacpGjOlpbyNY47Vwr6-8HxIm9lkYI/edit?gid=305644033#gid=305644033) |
| 📝 기획서 | 배경, 사용자 문제, 기능과 서비스 흐름 | [기획서 열기](https://docs.google.com/document/d/1-YzN9Gbwpwyt9RQgWLjE3HoCDPSbW14VlSZtkL0kwyA/edit?tab=t.0) |
| 🖼️ 화면 설계서 | 화면과 사용자 상호작용 정의 | 🔗 추후 링크 추가 |
| 🗃️ ERD | 핵심 Entity와 관계 | [ERD 열기](https://claude.ai/artifact/8c1WMx1Eji4CSEDgtes71e) |
| 📡 API 명세서 | Frontend·Spring·AI Backend의 요청·응답 계약 | 🔗 추후 링크 추가 |

---

## 8. 🚀 Local 실행

<details open>
<summary><b>▶️ Database와 Backend 실행</b></summary>

~~~bash
cp .env.example .env
docker compose up -d

# Windows
./mvnw.cmd spring-boot:run

# macOS · Linux
./mvnw spring-boot:run
~~~

기본 주소는 <code>http://localhost:8081</code>, Frontend는 <code>http://localhost:4173</code>입니다.

</details>

<details>
<summary><b>🔗 세 저장소를 함께 실행할 때</b></summary>

| Service | 기본 주소 | 역할 |
|---|---|---|
| Frontend | <code>http://localhost:4173</code> | 사용자 화면 |
| Spring Backend | <code>http://localhost:8081</code> | 인증·권한과 핵심 API |
| AI Backend | <code>http://localhost:8000</code> | 내부 AI API |
| PostgreSQL | <code>localhost:5433</code> | 공유 Database |

</details>

---

## 9. 🔐 환경 변수와 보안

<details open>
<summary><b>⚙️ 핵심 환경 변수</b></summary>

~~~dotenv
SERVER_PORT=8081
FRONTEND_ORIGIN=http://localhost:4173
DB_URL=jdbc:postgresql://127.0.0.1:5433/dodam
DB_USER=dodam
DB_PASSWORD=dodam_local
JWT_SECRET=change-me-to-a-long-random-string-at-least-32
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
AI_BASE_URL=http://localhost:8000
AI_INTERNAL_TOKEN=
DODAM_STORAGE_PROVIDER=local
STORAGE_LOCAL_ROOT=./data/object-storage
DODAM_STORAGE_S3_BUCKET=
DODAM_STORAGE_S3_REGION=
~~~

전체 항목은 [.env.example](./.env.example)을 기준으로 확인합니다.

</details>

<details>
<summary><b>🛡️ 운영 보안 Check</b></summary>

- Secret, Password, Firebase Credential과 내부 Token을 Commit하지 않습니다.
- 운영 HTTPS에서는 <code>JWT_COOKIE_SECURE=true</code>를 사용합니다.
- Private S3 Bucket의 Public Access를 차단합니다.
- 두 Backend는 충분히 긴 동일한 내부 Token을 사용합니다.
- 의료 문서 원문은 사용자와 아이의 권한 확인 후 제공합니다.
- Demo Login과 Debug 기능은 운영에서 비활성화합니다.

</details>

---

## 10. ✅ Test와 검증

<details open>
<summary><b>🧪 실행 명령어</b></summary>

~~~bash
# Windows
./mvnw.cmd verify

# macOS · Linux
./mvnw verify
~~~

</details>

<details>
<summary><b>📋 주요 검증 범위</b></summary>

- JWT, OAuth2, Cookie와 Security Contract
- 사용자·아이·보호자 관계와 접근 권한
- 기록·방문·성장 CRUD
- 문서 제한, 중복 처리, Local/S3와 원문 권한
- AI 요청, 상담 Transaction과 Citation
- 일정 승인·거절, 반복 복약과 알림
- 예방접종과 복지 추천 규칙
- Flyway Migration과 Database Constraint

</details>

---

## 11. 🤝 개발 컨벤션

<details open>
<summary><b>🌿 Branch Convention과 구조</b></summary>

[Branch Convention 전체 문서](./docs/convention/BRANCH_CONVENTION.md)

~~~text
main                         # 배포 가능한 안정 Branch
 └─ develop                  # 기능 통합·검증 Branch
     ├─ feat/12-short-name   # 새 기능
     ├─ fix/18-short-name    # Bug 수정
     ├─ refactor/23-name     # 구조 개선
     ├─ docs/25-name         # 문서
     ├─ test/29-name         # Test
     └─ chore/31-name        # 설정·의존성·Data 정리

hotfix/35-short-name         # main 기준 긴급 운영 수정
 ├─ main 반영
 └─ develop에도 동일 변경 반영
~~~

~~~text
Branch 이름: <type>/<issue-number>-<short-kebab-case-description>
예시: feat/12-child-care-memo-time
~~~

</details>

<details>
<summary><b>📝 Commit Convention과 구조</b></summary>

[Commit Convention 전체 문서](./docs/convention/COMMIT_CONVENTION.md)

~~~text
<type>(<scope>): <english subject>

<선택: 변경 이유와 구현 방식>
<선택: Refs: #issue-number>
~~~

~~~text
feat(health-record): persist selected record time
fix(auth): prevent expired token reuse
docs(readme): document backend architecture
test(care-relation): cover invite acceptance conflict
~~~

- Type, Scope와 Subject는 영어로 작성합니다.
- 하나의 Commit에는 하나의 논리적 변경만 담습니다.
- 최종 Squash Commit 제목도 영어 Convention을 지킵니다.

</details>

<details>
<summary><b>🔀 Pull Request Convention과 구조</b></summary>

[Pull Request Convention 전체 문서](./docs/convention/PR_CONVENTION.md)

~~~text
PR 제목
<type>(<scope>): <한국어 변경 내용>

예시
feat(health-record): 육아 메모 시간 선택 기능 추가
~~~

~~~markdown
## 무엇을 구현했나요?
- 사용자 또는 시스템 관점의 변경 사항

## 참고할 사항
- API·DB·환경 변수·연동 저장소 영향

## 테스트
- 실행한 명령과 수동 확인 결과
- 테스트하지 못한 항목과 사유

## 연결된 이슈
- Closes #이슈번호
~~~

~~~text
Issue 생성 → 최신 develop → 작업 Branch → Test
          → develop 대상 PR → CI 통과 → 1명 이상 Approve
          → Squash and merge → 작업 Branch 삭제
~~~

</details>

---

## 12. 🔗 도담 저장소 바로가기

| 저장소 | 담당 영역 | 바로가기 |
|---|---|---|
| 🎨 Frontend | React 화면, PWA, 사용자 상호작용과 Push 수신 | [Dodam_frontend](https://github.com/ieum-connect/Dodam_frontend) |
| ⚙️ Backend | 인증·권한, 핵심 Domain, 문서·AI 중계와 알림 | [Dodam_backend](https://github.com/ieum-connect/Dodam_backend) |
| 🤖 AI Backend | 문서 분석, RAG, AI 상담, 복지 설명과 접종 일정 | [Dodam_AI_backend](https://github.com/ieum-connect/Dodam_AI_backend) |

---

<div align="center">

### 아이의 기록과 가족의 돌봄을 안전하게 연결하는 도담 Backend ⚙️💗

</div>

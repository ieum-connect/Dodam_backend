# 도담(DODAM) 커밋 컨벤션

> 적용 대상: Frontend(React/Vite), Spring Backend, AI Backend(FastAPI)

## 1. 형식 및 언어

```text
<type>(<scope>): <english subject>

<선택: 변경 이유와 구현 방식>

<선택: 이슈 또는 호환성 정보>
```

- **커밋 제목의 `type`, `scope`, `subject`는 영어**로 작성합니다.
- `scope`는 선택 사항입니다. 예: `auth`, `visit`, `chat`, `document`, `ui`, `db`.
- 제목은 소문자로 시작하되 `AI`, `API`, `Dodam`, `Flyway` 등 고유명사·약어 표기는 유지할 수 있습니다.
- 제목 끝에 마침표를 붙이지 않고, 가능하면 72자 이내로 작성합니다.
- 제목은 작업 결과가 이해되는 간결한 표현을 사용합니다.
- 본문은 **한국어로 작성할 수 있습니다.** 변경 이유와 영향이 있을 때 자세히 적습니다.
- 하나의 커밋에는 되돌릴 수 있는 **하나의 논리적 변경**만 담습니다.

## 2. type 정의

| type | 사용 시점 |
| --- | --- |
| `feat` | 새 기능 추가 |
| `fix` | 버그·회귀 문제 수정 |
| `refactor` | 외부 동작이 바뀌지 않는 구조 개선 |
| `docs` | 문서·명세·주석만 변경 |
| `test` | 테스트만 추가·수정 |
| `chore` | 빌드·설정·의존성·시드 데이터 유지보수 |
| `style` | 동작에 영향 없는 코드 포맷 변경 |
| `perf` | 성능 개선 |
| `ci` | CI/CD 설정 변경 |
| `revert` | 기존 커밋 되돌리기 |

## 3. 좋은 예시

```text
feat(health-record): persist selected record time
fix(profile): keep profile sheet within viewport bounds
docs: add schema ERD generated from Flyway migrations
refactor(user): split read-only queries from commands
test(care-relation): cover invite acceptance conflict
chore: consolidate demo data
```

본문이 필요한 예시:

```text
fix(db): make child profile migration safe for existing data

기존 행을 먼저 보정한 뒤 NOT NULL 제약을 적용해
운영 데이터가 있는 환경에서도 마이그레이션이 실패하지 않게 한다.

Refs: #42
```

피해야 할 예시:

```text
수정
작업함
fix bug
feat: 여러 기능 추가 및 리팩터링
feat: 육아 메모 시간 선택 기능 추가
```

올바른 영어 제목:

```text
feat(health-record): allow selecting child care memo time
```

## 4. Breaking Change

기존 API·데이터 형식을 호환되지 않게 바꾸는 경우 `!`와 `BREAKING CHANGE:`를 사용합니다.

```text
feat(api)!: replace childName with childId

BREAKING CHANGE: 모든 자녀 조회 요청은 childId를 전달해야 한다.
```

## 5. 커밋 단위 및 사전 확인

- 포맷 변경과 기능 변경은 가능하면 분리합니다.
- 프론트 화면, API 계약, DB 마이그레이션 등 독립적으로 설명 가능한 변경은 커밋을 나눕니다.
- `dist/`, `target/`, 로그, 캐시, 불필요한 IDE 설정 및 `.env`를 커밋하지 않습니다.
- API 키, 토큰, 개인정보, 실사용자 의료 문서는 절대 커밋하지 않습니다.
- 대규모 이름 변경은 기능 변경과 분리합니다.
- 커밋 전 아래 명령으로 확인합니다.

```bash
git status
git diff --check
git diff --cached
```

- **PR 제목은 한국어**여도 되지만 **Squash and merge로 생성되는 최종 커밋 제목은 영어로 다시 작성**해야 합니다.
- 관련 Issue는 브랜치와 PR에서 연결하고, 필요하면 본문에 `Refs: #번호`를 적습니다.

# 도담(DODAM) 브랜치 컨벤션

> 적용 대상: `dodam-frontend`, `dodam-backend`, `dodam-ai` 전체 저장소

## 1. 기본 원칙

1. **개발 시작 전 GitHub Issue를 먼저 생성**합니다. 기능·버그·리팩터링·문서 등 작업 목적과 완료 조건을 작성합니다.
2. 하나의 브랜치는 하나의 Issue 또는 하나의 명확한 작업 목적을 담당합니다.
3. 작업 브랜치는 최신 `develop`에서 생성하고, **일반 PR의 대상 브랜치는 `develop`**으로 설정합니다.
4. `main`은 배포 가능한 안정 브랜치로 관리합니다. `develop`에서 검증한 변경을 배포할 때 팀이 승인한 PR로 `main`에 반영합니다.
5. `main`과 `develop`에 직접 푸시하지 않고 PR로 변경합니다. 운영 장애 등 긴급 대응은 예외 흐름을 팀과 공유합니다.

## 2. 브랜치 종류

| 브랜치 | 역할 |
| --- | --- |
| `main` | 배포 가능하고 검증이 끝난 안정 브랜치 |
| `develop` | 기능을 통합하고 테스트하는 기본 개발 브랜치 |
| 작업 브랜치 | 개별 Issue에 대응하는 단기 브랜치 |
| `hotfix/*` | 긴급 운영 수정용 브랜치. `main` 기준으로 만들 수 있으며 병합 후 `develop`에도 변경을 반영 |

## 3. 브랜치 네이밍

```text
<type>/<issue-number>-<short-kebab-case-description>
```

- 소문자 영문과 하이픈(`-`)을 사용합니다.
- 이슈를 먼저 생성하므로 가능하면 **Issue 번호를 포함**합니다.
- 설명은 짧되 작업 목적이 드러나게 작성합니다.

| type | 용도 | 예시 |
| --- | --- | --- |
| `feat` | 새 기능 | `feat/12-child-care-memo-time` |
| `fix` | 버그 수정 | `fix/18-profile-sheet-responsive` |
| `refactor` | 동작 변화 없는 구조 개선 | `refactor/23-user-query-service` |
| `docs` | 문서 변경 | `docs/25-team-convention` |
| `test` | 테스트 추가·수정 | `test/29-care-relation-service` |
| `chore` | 환경·의존성·데이터 정리 | `chore/31-reset-demo-data` |
| `hotfix` | 운영 긴급 수정 | `hotfix/35-login-redirect` |

`feature1`, `my-branch`, `test`처럼 용도가 불분명한 이름은 사용하지 않습니다.

## 4. 기본 작업 흐름

1. GitHub Issue를 만들고 **작업 배경, 요구사항, 완료 조건**을 작성합니다.
2. 최신 `develop`을 가져옵니다.
3. Issue 번호를 포함한 작업 브랜치를 만듭니다.
4. 관련 커밋을 작게 나눠 작성하고 테스트합니다.
5. `develop`을 대상으로 PR을 만들고 해당 Issue를 연결합니다.
6. **최소 1명의 승인**과 필수 검증을 받은 후 병합합니다.
7. 병합된 작업 브랜치는 삭제합니다.

```bash
git switch develop
git pull origin develop
git switch -c feat/12-child-care-memo-time
```

## 5. 협업 규칙

- 서로 다른 목적의 기능을 한 브랜치에 섞지 않습니다.
- 작업 브랜치에 불필요한 병합 커밋을 만들지 않습니다. 기준 브랜치 동기화 방식은 팀과 협의합니다.
- 공유된 커밋 이력을 임의로 덮어쓰지 않습니다. 강제 푸시 전 팀에 알립니다.
- 긴급 수정이 `main`에 먼저 반영되면 `develop`에도 누락 없이 반영합니다.
- 브랜치 규칙을 예외적으로 적용한 이유는 PR에 기록합니다.

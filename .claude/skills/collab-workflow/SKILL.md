---
name: collab-workflow
description: Guides klist 팀의 GitHub 협업 워크플로우를 이 저장소에서 실행할 때 사용한다. 사용자가 새 기능/버그/작업을 시작하자고 하거나, "이슈 만들어줘", "브랜치 파줘", "작업 시작할게", "커밋해줘", "PR 올려줘", "머지해줘" 처럼 이슈·브랜치·커밋·PR·머지 중 하나라도 언급하면 반드시 이 skill을 먼저 참고한다. 사용자가 워크플로우를 명시적으로 요구하지 않아도, 커밋이나 브랜치나 PR이 생기게 될 모든 작업 시작 시점에 이 skill을 확인해서 임의의 git 사용 대신 팀 컨벤션을 따르게 한다.
---

# klist 협업 워크플로우

이 저장소는 klist 팀의 GitHub 협업 컨벤션을 따른다. 이슈 생성부터 PR 머지까지 실제 작업 순서와 각 단계에서 실행할 명령을 아래에 안내한다.

## 원칙

- **이슈가 모든 작업의 시작점이다.** 아무리 작은 작업이어도 이슈 없이 브랜치를 만들거나 커밋하지 않는다.
- **각 단계는 순서대로 진행한다.** 이슈 생성 -> 브랜치 생성 -> (In Progress) 작업 -> 커밋 -> 푸시 -> PR 생성 -> PR 머지
- **GitHub에 실제로 영향을 주는 동작(이슈 생성, 푸시, PR 생성, PR 머지)은 사용자가 그 단계를 요청했을 때만 실행한다.** 

## 워크플로우

### 1. 이슈 생성

- 이 저장소(`gh repo view`로 현재 레포 확인 가능)에 이슈를 먼저 만든다.
- 제목은 Conventional Commits 타입 접두사를 사용한다.
- 라벨: 타입 라벨(`feat`/`fix`/`docs`/`refactor`/`chore`) 1개는 필수로 부착하고, 우선순위가 명확하면 `priority: high|medium|low`도 추가한다.
- 이슈 생성은 웹으로 유도한다. [kk-klist/.github](https://github.com/kk-klist/.github)의 이슈 폼(`feature.yml`/`bug.yml`)은 브라우저에서만 렌더링되고 필수 필드 검증도 웹에서만 적용되므로, CLI로 본문을 텍스트로 흉내내지 않고 아래 명령으로 템플릿 선택 화면을 열어 사용자가 직접 작성하도록 안내한다.

```bash
gh issue create --web
```

### 2. 브랜치 생성

- `develop`에서 분기하며, 브랜치명은 `feature/이슈번호-작업명` 또는 `fix/이슈번호-작업명` 형식을 따른다.

```bash
git checkout -b feature/12-login-api
```

### 3. 작업 시작 (In Progress)

- 코드 작업을 시작하는 즉시 보드에서 해당 이슈를 In Progress로 이동한다(수동)

### 4. 커밋

- Conventional Commits + 이슈 번호 형식을 지킨다: `feat: 로그인 API 연동 (#12)`
- 작은 단위로 자주 커밋한다.
- AI Agent 관련 내용은 포함하지 않는다.

### 5. 푸시

```bash
git push -u origin feature/12-login-api
```

- 푸시하는 순간 CI가 자동 실행된다.

### 6. PR 생성

- base는 `develop`. PR 하나는 하나의 목적만 갖는다(여러 기능을 묶지 않는다).
- 본문은 [kk-klist/.github](https://github.com/kk-klist/.github)가 제공하는 PR 템플릿(작업 내용 / 변경 사항 / 관련 이슈) 구조를 따르고, `Closes #이슈번호`를 반드시 포함한다(누락 시 머지해도 이슈가 자동으로 닫히지 않는다). 이 저장소에 별도 `.github/pull_request_template.md`를 만들지 않는다.
- 리뷰어를 1명 이상 지정한다.

```bash
gh pr create --base develop \
  --title "feat: 로그인 API 연동 (#12)" \
  --body "## 작업 내용
-

## 변경 사항
-

## 관련 이슈
Closes #12" \
  --reviewer <리뷰어>
```

- PR을 올린 뒤 보드에서 이슈를 In Review로 이동한다(수동).

### 7. PR 머지

- 머지 전에 **CI 통과 여부**와 **승인(Approve) 1개 이상**을 확인한다. 둘 중 하나라도 충족되지 않았으면 머지하지 않고 사용자에게 알린다.
- 머지 방식은 squash로 한다.

```bash
gh pr checks <PR번호>
gh pr merge <PR번호> --squash
```

## 하지 말아야 할 것

- `main` 브랜치에 직접 push하거나, PR 없이 머지하지 않는다(브랜치 보호 규칙 대상).
- 이슈 없이 브랜치를 만들거나 커밋하지 않는다.
- CI 실패 상태나 승인 없는 상태에서 머지하지 않는다.
- 라벨을 임의로 생성하지 않는다.
- 이슈/PR 템플릿을 이 저장소에 새로 만들지 않는다. [kk-klist/.github](https://github.com/kk-klist/.github)에서 조직 공통으로 관리한다.

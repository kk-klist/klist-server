# AGENTS.md

이 저장소는 `.claude/skills/`에 작업 종류별 가이드(Skill)를 두고 있다. Claude Code는 이 디렉토리를 자동으로 인식하지만, 그 외 에이전트(Codex 등)는 작업을 시작하기 전에 아래 목록에서 관련 Skill이 있는지 직접 확인하고, 있다면 해당 SKILL.md를 읽고 그 안내를 따른다.

## 사용 가능한 Skill

| Skill | 위치 | 사용 시점 |
|---|---|---|
| collab-workflow | [.claude/skills/collab-workflow/SKILL.md](.claude/skills/collab-workflow/SKILL.md) | 이슈 생성, 브랜치 생성, 커밋, 푸시, PR 생성, PR 머지 등 GitHub 협업 워크플로우와 관련된 작업을 할 때 |

새 Skill이 `.claude/skills/`에 추가되면 이름과 사용 시점을 이 표에 함께 추가한다. 표가 최신이 아닐 수 있으니 확실하지 않으면 `.claude/skills/` 아래를 직접 확인한다.

## 팀 컨벤션 문서

패키지 구조, 레이어별 코딩 규칙, 협업 워크플로우 등 klist 팀의 컨벤션은 [docs/README.md](docs/README.md)에 목록이 정리되어 있다. 코드를 작성하거나 협업 규칙(라벨, 브랜치 네이밍, PR 규칙 등)이 필요할 때는 이 목록에서 관련 문서를 찾아 참고한다.

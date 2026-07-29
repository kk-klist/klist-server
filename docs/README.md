# Spring Boot 팀 컨벤션

이 폴더는 Spring Boot 팀 컨벤션에 관련된 SSoT이다.

## 목차

| 문서 | 내용 |
|---|---|
| [package-structure.md](./package-structure.md) | 도메인 중심 패키지 구조 |
| [layer-convention/controller.md](./layer-convention/controller.md) | Controller 규칙 |
| [layer-convention/service.md](./layer-convention/service.md) | Service 규칙 |
| [layer-convention/entity.md](./layer-convention/entity.md) | Entity 규칙 |
| [layer-convention/repository.md](./layer-convention/repository.md) | Repository 규칙 |
| [layer-convention/dto.md](./layer-convention/dto.md) | DTO 규칙 |
| [response-structure.md](./response-structure.md) | API 성공/페이징 응답 구조 |
| [exception-handling.md](./exception-handling.md) | 예외 클래스, GlobalExceptionHandler, 로깅, 트랜잭션-예외 관계, HTTP 상태코드/ErrorCode 매핑 |
| [unit-test-convention.md](./unit-test-convention.md) | 단위 테스트 원칙, 네이밍, 픽스쳐, 레이어별 테스트 지침 |
| [querydsl-convention.md](./querydsl-convention.md) | QueryDSL 사용 규칙 |
| [collaboration-convention.md](./collaboration-convention.md) | 이슈·라벨·브랜치·커밋·PR 규칙, 칸반 보드 운영 방식 |

## 문서 관리 원칙

- 각 파일 상단의 `<!-- source -->` / `<!-- synced -->` 주석으로 원본 Notion 페이지와 마지막 동기화 시점을 추적합니다.
- 컨벤션 문서 수정은 코드 생성 결과에 직접 영향을 주므로 PR 리뷰를 거치는 것을 권장합니다.

## 변경 이력

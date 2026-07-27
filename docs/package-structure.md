<!-- source: https://app.notion.com/p/3a3a0d58dc6081d79debe845780f1c43 -->
<!-- synced: 2026-07-23 -->

# 패키지 구조

도메인 중심(Domain-Driven) 패키지 구조를 사용한다.

```
com.example.app
├── global/
│   ├── config/
│   ├── exception/
│   ├── response/
│   ├── security/
│   └── util/
└── domain/
    └── {도메인명}/
        ├── controller/
        ├── service/
        ├── domain/
        │   ├── entity/
        │   └── exception/
        ├── dto/
        │   ├── request/
        │   └── response/
        └── repository/
            ├── {Domain}JpaRepository.java
            ├── {Domain}Repository.java
            └── {Domain}RepositoryImpl.java
```

| 디렉토리 | 역할 |
|---|---|
| `global` | 전역 설정, 공통 유틸, 예외 처리, Base 클래스 등 어떤 도메인에도 종속되지 않는 코드를 담는다. |
| `controller` | 외부(클라이언트)의 HTTP 요청을 받는 Controller 클래스를 담는다. |
| `service` | 유스케이스 단위 비즈니스 흐름을 처리하는 Service 클래스를 담는다. |
| `domain` | 외부 기술(JPA, HTTP 등)에 의존하지 않는 도메인의 핵심 비즈니스 로직(Entity, Value Object, Exception)을 담는다. |
| `entity` | 식별자(ID)를 갖고 생명주기를 가지는 Entity 클래스를 담는다. |
| `exception` | 해당 도메인에서만 발생하는 예외 클래스를 담는다. |
| `dto/request` | API 요청 페이로드를 담는 요청 DTO 클래스를 담는다. |
| `dto/response` | API 응답 페이로드를 담는 응답 DTO 클래스를 담는다. |
| `repository` | 영속성 기술과 맞닿는 Repository 관련 인터페이스 및 구현 클래스를 담는다. |
| `{Domain}JpaRepository` | `JpaRepository`를 상속받아 기본 CRUD를 제공받는 Spring Data JPA 인터페이스를 담는다. |
| `{Domain}Repository` | Service가 의존하는 Repository 인터페이스를 담는다. |
| `{Domain}RepositoryImpl` | `{Domain}Repository`를 구현하는 클래스를 담는다. |

> ※ Repository 3분할 구조의 상세 규칙은 [`layer-convention/repository.md`](./layer-convention/repository.md) 참고

## 필수 규칙

- `global`은 어떤 `domain`도 알아서는 안 된다.
  - `global/exception`에는 `BusinessException`처럼 도메인 이름을 모르는 상위 타입만 둔다.
  - `OrderNotFoundException` 같은 도메인별 예외는 반드시 `domain/{domain}/exception`에 둔다.
  - 도메인 간 중복 로직은 `global`이 아닌 `domain/common` 같은 별도 공유 도메인으로 분리한다.

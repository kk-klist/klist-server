<!-- source: https://app.notion.com/p/3a3a0d58dc6081acb72ae832ffa0b2ff -->
<!-- synced: 2026-07-23 -->

# 단위 테스트 컨벤션

## 1. 핵심 원칙 지침
- **결정적(Deterministic)**: 실행 시점마다 달라지는 값을 코드에서 직접 호출하지 않는다. 커스텀 인터페이스로 추상화하고 생성자 주입으로 받아라.
- **빠름(Fast)**: DB, 네트워크 등 외부 시스템 없이 동작하도록 작성하라. Spring Context를 띄우지 않는다.
- **독립적(Isolated)**: 테스트 간 상태를 공유하지 않으며, 각 테스트는 스스로 필요한 상태를 셋업한다. 실행 순서에 따라 결과가 달라지는 테스트를 작성하지 않는다.
- **명확함(Readable)**: `@DisplayName`과 given/when/then 구조만 읽으면 무엇을 검증하는지 한 번에 이해할 수 있도록 작성한다.
- **추상화에 의존(Loose Coupling)**: 구현체가 아닌 인터페이스를 Mock하여 구현체가 바뀌어도 테스트가 깨지지 않게 한다.

## 2. 비결정적 의존성 추상화 지침
비결정적인 값을 생성하는 코드는 직접 호출하지 않는다. 커스텀 인터페이스로 추상화하고 생성자 주입으로 받는다.

| 비결정적 요소 | 직접 호출 ❌ | 추상화 후 주입 ✅ |
|---|---|---|
| 현재 시각 | `LocalDateTime.now()` | `TimeProvider` 인터페이스 |
| 고유 식별자 | `UUID.randomUUID()` | `UuidGenerator` 인터페이스 |
| 랜덤 값 | `new Random().nextInt()` | `RandomGenerator` 인터페이스 |

운영 환경에서는 실제 구현체를, 테스트에서는 고정 값을 반환하는 구현체로 교체한다.

## 3. 테스트 구조 — Given / When / Then
- 모든 테스트 메서드 내부는 이 순서를 **주석으로 구분**해서 작성한다.

| 단계 | 역할 |
|---|---|
| **given** | 테스트가 동작하기 위한 사전 조건과 Mock 설정 |
| **when** | 검증하려는 동작을 실행하고 결과를 변수에 담음 |
| **then** | 결과가 기댓값과 일치하는지 확인 |

when과 then의 경계가 코드 구조상 분리되지 않는 경우에만 `when & then`으로 합쳐 표기한다. 단순히 코드가 짧아서 합치는 것은 허용하지 않는다.
즉, `when`의 결과를 변수에 담을 수 있으면 분리하고 구문 구조상 실행과 결과 확인이 한 식에 묶이면 합친다.

## 4. 네이밍 규칙
- 메서드명: `대상_상황_기대결과` 형식으로 작성한다.

```java
createMember_whenDuplicateEmail_throwsDuplicateEmailException()
findMember_whenNotFound_throwsMemberNotFoundException()
changeName_whenValidName_updatesNameSuccessfully()
```

- `@DisplayName`: 주어는 생략하고 한국어로 `~하면 ~된다` 형태로 행위와 기대 결과를 명확하게 기술한다.

```java
@DisplayName("이미 비활성화된 회원를 다시 비활성화하면 InvalidStateException이 발생된다")
@DisplayName("정상적인 이름으로 변경하면 이름이 업데이트된다")
```

## 5. 테스트 픽스쳐

### 파일 위치
`src/test/java` 아래에 위치시키고 패키지는 테스트 대상 도메인과 동일하게 맞춰라.

```
src/test/java/com/example/app/
└── domain/
    └── member/
        └── fixture/
            ├── MemberFixture.java        # Entity 픽스쳐
            └── MemberDtoFixture.java     # DTO 픽스쳐
```

### Entity 픽스쳐
상태별로 메서드를 명확하게 분리한다. 메서드 이름만 보고 어떤 상태의 객체인지 바로 알 수 있어야 한다.
- 별도의 내부 static 메서드 `MemberFixture.Active`, `MemberFixture.Inactive` 등으로 그룹핑한다.

### DTO 픽스쳐
Entity 픽스쳐과 별도 파일로 분리하라. Request와 Response는 `{Domain}DtoFixture` 한 파일에서 함께 관리한다.

### 네이밍 규칙

| 유형 | 패턴 | 예시 |
|---|---|---|
| 기본 상태 | `{상태}{도메인}()` | `activeMember()`, `inactiveMember()` |
| 특정 값 주입 | `{기본상태}With{필드명}(값)` | `activeMemberWithEmail(email)` |
| 특수 케이스 | 상황을 명확히 서술 | `memberWithExpiredSubscription()` |

### 픽스쳐 주의사항
- **픽스쳐 안에 검증 로직 금지**: `assertThat`, `if` 같은 조건/검증 코드를 넣지 않는다.
- **픽스쳐끼리 의존 금지**: `OrderFixture`가 `MemberFixture`를 호출하는 구조를 만들지 않는다. 각 픽스쳐은 독립적으로 동작해야 한다.

## 6. Entity 단위 테스트 지침
- Spring Context, DB가 전혀 필요 없으므로 `@ExtendWith` 없이 작성한다.
- 다음 네 가지를 반드시 검증한다.
  1. **생성 불변식**: null 금지, 길이 제한 등 생성 시점의 조건
  2. **상태 전이**: 비즈니스 메서드 호출 후 상태가 올바르게 바뀌는지
  3. **유효하지 않은 상태 전이**: 허용되지 않는 전이 시 예외 발생 여부
  4. **비즈니스 규칙**: Entity 내부에 캡슐화된 정책 로직이 의도대로 동작 여부

## 7. Service 단위 테스트 지침
- `@ExtendWith(MockitoExtension.class)`를 사용하고 **`JpaRepository`가 아닌 `Repository` 인터페이스를 Mock**한다.
- 다음 네 가지를 반드시 검증한다.
  1. **정상 흐름**: 올바른 입력에 대해 Repository를 호출하고 응답을 올바르게 변환하는지
  2. **예외 흐름**: 비즈니스 규칙 위반 시 적절한 도메인 예외 발생 여부
  3. **Repository 호출 검증**: 올바른 인자로, 정확히 몇 번 호출되는지
  4. **DTO 변환**: Entity → Response DTO 변환이 누락 없이 되는지

## 8. Controller 단위 테스트 지침
- `@WebMvcTest` + `@MockBean`으로 Service를 Mock하고 **HTTP 요청/응답 레이어만** 검증한다.
- DB, 실제 비즈니스 로직은 Controller 테스트의 검증 대상이 아니다.
- 다음 다섯 가지를 반드시 검증하라:
  1. **HTTP 상태 코드**: 정상/예외 상황에 맞는 상태 코드가 내려오는지
  2. **요청 유효성 검증**: `@Valid` 위반 시 400이 반환되는지
  3. **응답 바디 구조**: JSON 필드명과 값이 기대한 형태로 직렬화되는지
  4. **Service 위임**: Controller가 올바른 인자로 Service를 호출하는지
  5. **예외 → HTTP 상태 매핑**: 도메인 예외가 `@ExceptionHandler`를 통해 올바른 상태 코드로 변환되는지

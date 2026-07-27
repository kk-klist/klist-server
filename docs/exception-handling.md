<!-- source: https://app.notion.com/p/3a3a0d58dc608107ba08c5692327d093 -->
<!-- synced: 2026-07-23 -->

# 예외 처리

## 1. 예외 클래스 규칙

### 계층 구조
- 모든 커스텀 예외는 `global/exception`의 `BusinessException`을 최상위 부모로 사용한다.
- 도메인별 예외는 `domain/{domain}/exception` 아래에 위치시키고 `BusinessException`을 상속한다.
- `Checked Exception`은 직접 만들어 사용하지 않는다. 모든 커스텀 예외는 `BusinessException`을 상속한 Unchecked Exception으로 만들어라.

```
global/exception/
└── BusinessException.java        ← 최상위 커스텀 예외 (abstract)

domain/member/exception/
├── MemberNotFoundException.java
└── DuplicateMemberEmailException.java
```

### ErrorCode 설계
- `ErrorCode`는 도메인별로 분리하며, 전역 공통 코드만 `global/exception`에 두고 나머지는 각 도메인 패키지에 위치시킨다.
- 각 `ErrorCode` enum은 HTTP 상태코드, 에러 식별 코드, 메시지를 함께 관리한다.

```
global/exception/
└── GlobalErrorCode.java         ← 공통 에러 (서버 오류, 인증 오류 등)

domain/member/exception/
└── MemberErrorCode.java         ← 회원 도메인 에러
```

#### HTTP 상태 코드 기준 (4xx)

| 상황 | HTTP 상태 | ErrorCode 예시 |
|---|---|---|
| 리소스 없음 | 404 Not Found | `MEMBER_NOT_FOUND` |
| 중복 데이터 | 409 Conflict | `DUPLICATE_EMAIL` |
| 입력값 검증 실패 | 400 Bad Request | `INVALID_INPUT` |
| 인증 실패 | 401 Unauthorized | `INVALID_PASSWORD` |
| 권한 없음 | 403 Forbidden | `ACCESS_DENIED` |
| 지원하지 않는 메서드 | 405 Method Not Allowed | `METHOD_NOT_ALLOWED` |

#### HTTP 상태 코드 기준 (5xx)

| 상황 | HTTP 상태 | 처리 방식 |
|---|---|---|
| 외부 연동 실패 | 502 Bad Gateway | 도메인 `ErrorCode` 정의 후 `BusinessException` throw |
| 인프라 오류 (DB 등) | 500 Internal Server Error | 도메인 `ErrorCode` 정의, `log.error` 사용 |
| 예상치 못한 예외 | 500 Internal Server Error | `GlobalExceptionHandler`의 `Exception.class` 핸들러가 처리, `log.error` 사용 |

> ※ 성공 응답 및 `ErrorResponse` 바디 구조 자체는 [`response-structure.md`](./response-structure.md) 참고

## 2. 전역 예외 처리 (GlobalExceptionHandler)
- `@RestControllerAdvice`를 사용해 모든 예외를 한 곳에서 처리한다.
- Controller에서 try-catch로 예외를 직접 처리하지 않는다.
- 다음 4가지 케이스는 반드시 핸들링한다.
  1. **도메인 비즈니스 예외** — `BusinessException.class`
  2. **`@Valid` 검증 실패** — `MethodArgumentNotValidException.class`
  3. **`@PathVariable`, `@RequestParam` 타입 불일치** — `MethodArgumentTypeMismatchException.class`
  4. **예상치 못한 예외 (fallback)** — `Exception.class`

## 3. 로깅 전략 지침

### 로그 레벨 기준

| 레벨 | 사용 기준 |
|---|---|
| `ERROR` | 즉시 대응이 필요한 시스템 오류 (예상치 못한 Exception, 외부 연동 실패) |
| `WARN` | 비즈니스 예외, 재시도 가능한 오류 (BusinessException) |
| `INFO` | 주요 비즈니스 흐름 (회원 가입, 주문 생성 등 의미 있는 이벤트) |
| `DEBUG` | 개발/디버깅용 상세 정보. **운영 환경에서는 출력하지 않는다.** |

### 로그 작성 규칙
- 코드에서는 **항상 SLF4J 인터페이스를 사용한다.** `Log4j2` 구현체를 직접 import하지 않는다.
- **예외 로깅은 `GlobalExceptionHandler`에서만 한다.** Service에서 예외를 catch해 로그를 남기고 다시 throw하면 같은 예외가 두 번 로깅된다.
- **문자열 연결(+) 대신 파라미터 바인딩을 사용한다.**

```java
// ❌ 금지
log.info("회원 가입 완료: " + member.getId());

// ✅ 올바른 사용
log.info("[Member] 회원 가입 완료. memberId={}", member.getId());
```

- **로그 메시지는 `[도메인] 동작. key=value` 형식으로 통일한다.**

```java
log.info("[Member] 회원 가입 완료. memberId={}", member.getId());
log.warn("[Order] 재고 부족. productId={}, requestedQty={}", productId, qty);
log.error("[Payment] 결제 실패. orderId={}", orderId);
```

- **민감 정보는 로그에 포함하지 않는다** (`password`, 카드번호 등).

## 4. 트랜잭션과 예외의 관계 지침

### 트랜잭션 경계 안에서 예외를 삼키지 마라
예외를 catch만 하고 삼키면 롤백이 일어나지 않아 데이터 불일치가 발생한다.

```java
// ❌ 금지: 예외 삼키기
@Transactional
public void createOrder(OrderCreateRequest request) {
    try {
        orderRepository.save(order);
        paymentService.pay(order);
    } catch (Exception e) {
        log.error("결제 실패", e);
    }
}
```

### 외부 I/O와 DB 작업 분리
외부 I/O(파일, 이메일, FTP 등)와 DB 작업을 같은 트랜잭션 안에 두지 않는다.
외부 작업은 DB 트랜잭션 커밋 이후에 실행하고, 실패 시 보상 로직으로 처리하라.

### 트랜잭션 범위 주의사항
`@Transactional`이 붙은 메서드를 **같은 클래스 내부에서 호출하면 트랜잭션이 적용되지 않는다.** (Spring AOP 프록시 우회)

```java
// ❌ 금지: 내부 호출로 트랜잭션 미적용
@Service
public class OrderService {
    public void createOrder(OrderCreateRequest request) {
        processOrder(request);  // 트랜잭션 적용 안 됨
    }

    @Transactional
    public void processOrder(OrderCreateRequest request) { ... }
}

// ✅ 올바른 사용: 진입점 메서드에 @Transactional 선언
@Service
public class OrderService {
    @Transactional
    public void createOrder(OrderCreateRequest request) { ... }
}
```

> ※ Service 레이어의 `@Transactional` 기본 배치 규칙은 [`layer-convention/service.md`](./layer-convention/service.md) 참고

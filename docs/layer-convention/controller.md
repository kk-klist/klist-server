<!-- source: https://app.notion.com/p/3a3a0d58dc6081c69be3feaf05ab8ca3 (레이어별 코딩 컨벤션 중 Controller 섹션) -->
<!-- synced: 2026-07-23 -->

# Controller 컨벤션

```java
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<MemberCreateResponse> createMember(
            @Valid @RequestBody MemberCreateRequest request
    ) {
        MemberCreateResponse response = memberService.createMember(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
```

## 역할 범위
- HTTP 요청을 받아 Service에 위임하고, 응답을 변환해서 내려주는 것까지만 담당한다.
- 비즈니스 로직(조건 분기, 계산, 정책 판단)은 절대 다루지 않는다.
- 요청과 응답은 `dto/request`, `dto/response`의 DTO를 통해서만 주고받는다.
- 예외는 직접 처리하지 않고 GlobalExceptionHandler로 위임한다. (상세: [`exception-handling.md`](../exception-handling.md))

## 클래스/메서드 규칙
- 클래스명은 `{Domain}Controller` 형식만 사용한다.
- `@RestController` + `@RequestMapping("/api/v1/{domain}")` 형태로 API 버전을 명시한다.
- 생성자 주입(`@RequiredArgsConstructor` + `private final`)만 사용하고 필드 주입(`@Autowired` on field)은 금지한다.
- 응답은 `global/response`에 정의된 공통 응답 포맷(`ApiResponse<T>` 등)으로 통일한다.
- 예외는 try-catch로 직접 잡아 처리하지 않고 GlobalExceptionHandler로 위임한다. (상세: [`exception-handling.md`](../exception-handling.md))

## ResponseEntity 사용 규칙
- 조회(GET) 성공은 `ResponseEntity.ok(...)`를 사용한다.
- 생성(POST) 성공은 `ResponseEntity.status(HttpStatus.CREATED).body(...)`를 사용한다.
- 수정(PUT/PATCH)·삭제(DELETE) 성공은 body 없이 `ResponseEntity.noContent().build()` (204 No Content)를 사용한다. (상세: [`response-structure.md`](../response-structure.md))
- 위 방식으로 표현되지 않는 상태 코드(`ResponseEntity.status(임의 값)`)를 새로 도입하는 것을 금지하고, 필요한 상황이 생기면 팀 논의 후 이 규칙에 추가한다.
- `ResponseEntity<T>`의 `T`는 항상 `dto/response`의 DTO만 사용하고 `Map`이나 `String` 같은 비정형 타입을 반환 타입으로 쓰는 것을 금지한다.
- 실패 응답(4xx/5xx)의 경우 메서드 내부에서 `ResponseEntity.badRequest()` 같은 에러 응답을 직접 만드는 것을 금지한다.

## 검증
- Controller 메서드 내부에서 `if (request.getName() == null)` 같은 수동 검증 코드는 금지한다.
- `@RequestBody`의 검증은 `@Valid` + Request DTO 내부 어노테이션(`@NotNull`, `@Size` 등)으로 처리한다.
- `@PathVariable`의 검증은 `Long`, `UUID` 등 명확한 타입을 지정 후 값의 범위나 형식에 제약이 필요하면 클래스에 `@Validated`를 붙이고 파라미터에 제약 어노테이션(`@Positive` 등)을 직접 건다.

## 인증 정보
- 로그인한 사용자의 식별자는 커스텀 `@LoginUser` 어노테이션으로 `Long userId`를 직접 받는다.
- Controller에서 `SecurityContextHolder`를 직접 호출하거나 토큰을 파싱하는 코드, `CustomUserDetails`를 직접 받는 코드는 금지한다.

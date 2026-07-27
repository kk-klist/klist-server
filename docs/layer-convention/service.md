<!-- source: https://app.notion.com/p/3a3a0d58dc6081c69be3feaf05ab8ca3 (레이어별 코딩 컨벤션 중 Service 섹션) -->
<!-- synced: 2026-07-23 -->

# Service 컨벤션

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;          // 같은 도메인 - 직접 의존 OK
    private final OrderItemRepository orderItemRepository;  // 같은 도메인 - 직접 의존 OK
    private final MemberService memberService;              // 다른 도메인 - Repository 대신 Service로 접근

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        MemberDetailResponse member = memberService.findMember(request.getMemberId());
        // 같은 도메인 내 Repository들을 조합해 주문 생성 로직 수행
        // ...
    }
}
```

## 역할 범위
- 비즈니스 로직, 트랜잭션 경계, 같은 도메인 내 여러 Repository를 조합하는 책임을 지는 것까지 담당한다.
- 다른 도메인의 데이터가 필요한 경우, 해당 도메인의 Repository를 직접 주입받아 사용하는 것은 절대 하지 않는다.
- 다른 도메인 접근은 해당 도메인의 Service를 통해서만 한다.
- Request DTO → Entity, Entity → Response DTO 변환의 책임도 Service가 가진다.

## 클래스/메서드 규칙
- 클래스명은 `{Domain}Service` 형식만 사용한다.
- 다중 구현체가 필요한 특수 케이스를 제외하고 인터페이스 없이 구현 클래스 하나로만 작성한다.
- 메서드명은 동사로 시작하는 이름만 사용한다 (`createMember`, `findMemberById`, `deleteMember` 등).

## 트랜잭션
- 클래스 레벨에 `@Transactional(readOnly = true)`를 기본으로 걸고, 쓰기 메서드에만 `@Transactional`을 메서드 레벨에 따로 명시한다.
- ※ 예외 발생 시 트랜잭션 처리 원칙은 [`exception-handling.md`](../exception-handling.md)의 "트랜잭션과 예외의 관계" 참고

## 예외 처리
- 도메인 의미를 가진 커스텀 예외(`domain/exception`)를 던지고, `RuntimeException`을 그대로 던지는 것은 금지한다. (상세: [`exception-handling.md`](../exception-handling.md))
- `Optional`은 Service 경계 안에서 `orElseThrow`로 해소하고, Controller까지 넘기는 것을 금지한다.

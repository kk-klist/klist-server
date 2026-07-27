<!-- source: https://app.notion.com/p/3a3a0d58dc6081c69be3feaf05ab8ca3 (레이어별 코딩 컨벤션 중 DTO 섹션) -->
<!-- synced: 2026-07-23 -->

# DTO 컨벤션

```java
public record MemberCreateRequest(
        @NotBlank
        @Size(max = 50)
        String name,

        @NotBlank
        @Email
        String email
) {
}

public record MemberCreateResponse(
        Long id,
        String name,
        String email
) {
    public static MemberCreateResponse from(Member member) {
        return new MemberCreateResponse(
                member.getId(),
                member.getName(),
                member.getEmail()
        );
    }
}
```

## 역할 범위
- Controller와 Service 사이에서 주고받는 데이터의 형태(입력 형식 또는 출력 형태)를 고정하는 것까지만 담당한다.
- 비즈니스 로직(조건 분기, 정책 판단)은 절대 갖지 않는다.
- Entity와는 별도 타입으로 존재하며, Entity ↔ DTO 변환은 정적 팩토리 메서드(`from`, `of` 등)를 통해서만 이루어진다.

## 클래스/필드 규칙
- 클래스명은 `{Domain}{Action}Request` / `{Domain}{Action}Response` 형식만 사용한다 (예: `MemberCreateRequest`, `MemberDetailResponse`).
- Request/Response 모두 `record`로만 작성하고, 별도의 Getter/생성자를 직접 구현하지 않는다.
- Request DTO의 필드 검증은 Bean Validation 어노테이션(`@NotBlank`, `@Size`, `@Email` 등)으로만 표현하고, 메서드 본문에 수동 검증 코드를 두는 것은 금지한다.
- Response DTO는 Entity를 인자로 받는 정적 팩토리 메서드(`from(Entity entity)`)로만 생성하고, 생성자를 외부에서 직접 호출하는 것은 금지한다.
- 응답 구조는 중첩이 필요하면 별도의 내부 record 타입으로 분리하고, `Map<String, Object>`나 원시 타입 조합으로 표현하는 것을 금지한다.

## Validation 시나리오 분리
- 생성/수정처럼 시나리오별로 검증 규칙이 다르면 Bean Validation Group으로 같은 Request를 분기하지 않고, `{Domain}CreateRequest`/`{Domain}UpdateRequest`처럼 별도의 클래스로 분리한다.

## 리스트/페이징 응답
목록 응답은 **페이징 여부**에 따라 반환 타입을 구분한다.

| 상황 | 반환 타입 | 예시 |
|---|---|---|
| 페이징이 필요한 목록 (대량 데이터, 무한 스크롤, 페이지 번호) | `PageResponse<T>` | 게시글 목록, 주문 내역 |
| 페이징이 필요 없는 목록 (소량 고정 데이터) | `List<T>` | 카테고리 목록, 드롭다운용 코드 리스트 |

- `PageResponse<T>`는 `global/response`에 정의된 공통 포맷만 사용한다. (상세: [`response-structure.md`](../response-structure.md))
- 단건 응답 DTO를 그대로 목록에 재사용하는 것을 금지한다 (예: `MemberDetailResponse`를 목록에 그대로 쓰지 않고 `MemberSummaryResponse`처럼 별도로 정의한다).

<!-- source: https://app.notion.com/p/3a3a0d58dc6081c28a1ed431966b8b0a -->
<!-- synced: 2026-07-23 -->

# 응답 구조

## 1. 성공 응답 — `ApiResponse<T>`
- 모든 성공 응답은 `global/response`에 정의된 `ApiResponse<T>`로 통일한다.
- `success` 필드는 **애플리케이션 비즈니스 처리 성공 여부**를 나타내며, HTTP 상태코드와 역할이 구분된다.

### GET — 단건 조회
리소스 하나를 `data` 필드에 객체로 직접 포함한다.

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<MemberResponse>> getMember(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(memberService.findMember(id)));
}
```

### GET — 목록 조회 (Non-Paged)
페이징 없이 전체 목록을 반환할 때는 `data` 필드에 배열로 포함한다.

### POST — 생성
생성된 리소스 전체를 반환하여 클라이언트가 생성 직후 해당 리소스를 바로 참조할 수 있도록 한다.

```java
@PostMapping
public ResponseEntity<ApiResponse<MemberCreateResponse>> createMember(
        @Valid @RequestBody MemberCreateRequest request) {
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(memberService.createMember(request)));
}
```

### PUT / PATCH — 수정
수정 요청 시 클라이언트는 이미 대상 ID를 알고 있으므로 응답 body 없이 `204 No Content`만 반환한다.

```java
@PatchMapping("/{id}")
public ResponseEntity<Void> updateMember(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest request) {
    memberService.updateMember(id, request);
    return ResponseEntity.noContent().build();
}
```

### DELETE — 삭제
삭제 요청 시 클라이언트는 이미 대상 리소스의 ID를 알고 있으므로 응답 body 없이 `204 No Content`만 반환한다.

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
    memberService.deleteMember(id);
    return ResponseEntity.noContent().build();
}
```

> ※ Controller 레이어에서의 `ResponseEntity` 사용 규칙 전반은 [`layer-convention/controller.md`](./layer-convention/controller.md) 참고

## 2. 페이징 응답 지침 — `PageResponse<T>`
- Spring Data JPA의 `Page<T>` / `Slice<T>`를 그대로 직렬화하지 않는다.
- `ApiResponse<PageResponse<T>>`와 같이 이중 래핑 형태로 사용한다.
- `global/response`에 정의된 `PageResponse<T>` 공통 포맷만 사용하라.

### 페이징 방식 (Page 기반)
count 쿼리가 실행되어 `totalElements` / `totalPages`가 함께 내려온다.

```java
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<MemberResponse>>> searchMembers(
        MemberSearchCondition condition,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(
            ApiResponse.success(memberService.searchMembers(condition, pageable))
    );
}
```

### 커서 방식 (Slice 기반)
count 쿼리 없이 다음 페이지 존재 여부만 확인한다. `totalElements` / `totalPages`는 응답에 포함되지 않는다.

### 페이징 요청 파라미터 규칙
별도 파싱 코드를 작성하지 말고 컨트롤러 파라미터에 `Pageable pageable`만 선언한다.

```
GET /api/v1/members?page=0&size=20&sort=createdAt,desc
```

## 3. 실패 응답 지침 — `ErrorResponse`
- `code`는 프론트 분기 로직의 기준값이므로 **클라이언트와 사전 합의 후에만 변경**한다.
- `message`는 자유롭게 수정할 수 있지만, 프론트가 분기 로직에 사용하지 않도록 한다.
- `errors` 필드는 입력값 검증 실패 시에만 포함한다.

> ※ 상황별 HTTP 상태 코드 / ErrorCode 매핑 표는 [`exception-handling.md`](./exception-handling.md)의 "ErrorCode 설계" 참고

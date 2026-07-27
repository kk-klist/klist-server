<!-- source: https://app.notion.com/p/3a5a0d58dc6080bdb6f7ed838d59284a -->
<!-- synced: 2026-07-23 -->

# QueryDSL 컨벤션

> ※ `{Domain}RepositoryImpl` 전반의 역할/구조는 [`layer-convention/repository.md`](./layer-convention/repository.md) 참고

## 1. JPAQueryFactory 주입 규칙
- `EntityManager`를 직접 주입받아 `RepositoryImpl` 내부에서 `new JPAQueryFactory(em)`을 생성하는 것을 금지한다.
- QueryDSL은 `{Domain}RepositoryImpl`에서만 생성자 주입으로 사용하고, Service나 다른 레이어에서 `JPAQueryFactory`를 직접 주입받는 것을 금지한다.

## 2. Q타입 사용 규칙
- Q타입은 `static import`로 파일 상단에 정적 임포트하고, 메서드 내부에서 지역 변수로 선언하는 것을 금지한다.
- 동일한 Entity를 Self Join 해야 하는 경우에만 예외적으로 별칭을 사용한다.
- Self Join 시 별칭은 Entity명 + 역할 또는 관계를 명확히 표현하는 이름으로 짓는다.

## 3. 동적 쿼리 작성 규칙

### 조건 메서드 분리 규칙
- 동적 조건은 반드시 `BooleanExpression`을 반환하는 `private` 메서드로 분리한다.
- `.where()` 블록 안에서 직접 삼항 연산자(`condition != null ? ... : null`)를 작성하는 것을 금지한다.
- 조건 메서드명은 `{필드명}{조건동사}` 형태로 짓는다 (`nameContains`, `statusEq`, `ageBetween` 등).
- `null`을 반환하는 `BooleanExpression`은 QueryDSL이 자동으로 무시하므로, 조건이 없을 때 `null`을 반환하는 것을 허용한다.
- `BooleanBuilder`는 조건 조합 의도가 명확히 드러나지 않으므로 사용을 금지하고, `BooleanExpression` 조합으로 대체한다.

### 조건 조합 규칙
- 여러 `BooleanExpression`을 AND 조합할 때는 `.where()`에 `,`로 나열한다.
- OR 조합이 필요한 경우 명시적으로 `.or()`를 사용한다.

## 4. Projection 사용 규칙

### Projection 선택 기준

| 상황 | 방식 | 이유 |
|---|---|---|
| 자주 쓰이는 조회용 Response DTO | `@QueryProjection` | 컴파일 타임 타입 안전성 보장 |
| 단순 집계·통계성 쿼리 | `Projections.constructor` | Q타입 생성 비용 대비 효과 낮음 |
| Entity 전체가 필요한 경우 | `selectFrom` + fetch join | Projection 대신 Entity 조회 |

- `Projections.fields`와 `Projections.bean`은 사용을 금지한다.
- `@QueryProjection`을 적용한 DTO는 `dto/response` 패키지에 두고 Q타입은 자동 생성에 맡긴다.
- 단, 도메인 레이어 DTO는 기술 스택에 독립적이어야 하므로 적용 대상에서 제외한다.

## 5. 페이징 처리 규칙

### count 쿼리 규칙
- 페이징 처리 시 content 쿼리와 count 쿼리는 항상 분리해서 작성한다.
- `fetchResults()`와 `fetchCount()`는 Deprecated 되었으므로 사용을 금지한다.
- count 쿼리는 `PageableExecutionUtils.getPage()`를 사용해 최적화한다.
- count 쿼리의 `select`절은 `member.count()`처럼 집계 함수만 두고, content 쿼리의 `orderBy`나 불필요한 join은 포함하지 않는다.

## 6. 성능 주의사항

### fetch join 규칙
- N+1이 예상되는 단건/ToOne 연관관계 조회에는 `fetchJoin()`을 명시한다.
- 컬렉션(`OneToMany`) fetch join과 페이징을 동시에 사용하는 것을 금지한다. 컬렉션이 필요한 경우 `default_batch_fetch_size`를 활용하거나 별도 쿼리로 분리한다.
- 동일 쿼리에서 둘 이상의 컬렉션을 `fetchJoin()`하는 것을 금지한다.
- `fetchJoin()`이 필요한 이유를 메서드 또는 PR 설명에 주석으로 남긴다.

### 기타 성능 규칙
- 조회 전용 쿼리는 반드시 `@Transactional(readOnly = true)` 트랜잭션 내에서 실행한다.
- `select`절에 불필요한 컬럼을 포함하지 않는다. 특히 Entity 전체가 필요하지 않은 집계·목록 쿼리는 Projection을 적극 활용한다.
- 쿼리 결과가 단건임이 보장되는 경우 `fetchOne()`을 사용하고, 결과가 없을 수도 있는 경우 `Optional.ofNullable(queryFactory...fetchOne())`으로 감싸 반환한다. `fetch().stream().findFirst()` 사용을 금지한다.

## 7. RepositoryImpl 메서드 네이밍 규칙
- 단순 목록 조회: `findAll{Condition}` (`findAllByStatus`, `findAllByAgeRange`)
- 단건 조회: `find{Domain}By{Condition}` (`findMemberByEmail`)
- 동적 검색 + 페이징: `search{Domain}` (`searchMember`, `searchOrder`)
- 집계: `count{Condition}`, `sum{Field}By{Condition}`
- 존재 여부: `exists{Condition}` (`existsByNicknameAndStatus`)

## 8. exists 쿼리 최적화
- `fetchOne() != null` 방식은 전체 레코드를 조회하므로 사용을 금지한다.
- exists 여부 확인은 `selectOne()` + `limit(1)` + `fetchFirst()` 조합으로 작성한다.

## 9. 정렬(OrderBy) 처리 규칙
- 정렬 조건이 고정인 경우 쿼리 내에 직접 `.orderBy()`로 명시한다.
- 동적 정렬이 필요한 경우 `OrderSpecifier<?>[]`를 반환하는 `private` 메서드로 분리한다.
- `Pageable`의 `Sort`를 그대로 QueryDSL에 변환하는 경우 `PathBuilder`를 사용하되, 허용된 정렬 필드를 화이트리스트로 관리해 임의 필드 정렬을 방지한다.

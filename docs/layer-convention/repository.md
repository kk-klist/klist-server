<!-- source: https://app.notion.com/p/3a3a0d58dc6081c69be3feaf05ab8ca3 (레이어별 코딩 컨벤션 중 Repository 섹션) -->
<!-- synced: 2026-07-23 -->

# Repository 컨벤션

> ※ QueryDSL을 사용한 구현 세부 규칙은 [`querydsl-convention.md`](../querydsl-convention.md) 참고

```java
public interface MemberJpaRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
}

public interface MemberRepository {
    Optional<Member> findById(Long id);
    Member save(Member member);
    Page<Member> search(MemberSearchCondition condition, Pageable pageable);
}

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id);
    }

    @Override
    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }

    @Override
    public Page<Member> search(MemberSearchCondition condition, Pageable pageable) {
        QMember member = QMember.member;
        List<Member> content = queryFactory
                .selectFrom(member)
                .where(
                        nameContains(condition.getName()),
                        statusEq(condition.getStatus())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(member.count())
                .from(member)
                .where(
                        nameContains(condition.getName()),
                        statusEq(condition.getStatus())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? QMember.member.name.contains(name) : null;
    }

    private BooleanExpression statusEq(MemberStatus status) {
        return status != null ? QMember.member.status.eq(status) : null;
    }
}
```

## 역할 범위
- 영속성 계층에 대한 조회·저장 책임만 지는 것까지 담당한다.
- 비즈니스 로직(검증, 정책 판단)은 절대 다루지 않는다.
- Service는 `{Domain}Repository` 인터페이스를 통해서만 이 레이어에 접근하고, `JpaRepository`나 QueryDSL 같은 구현 세부사항은 알지 못한다.

## `{Domain}JpaRepository` 규칙
- `JpaRepository<Entity, ID>`를 상속하는 순수 Spring Data JPA 인터페이스로만 작성한다.
- 메서드 이름 규칙(`findByEmail`, `existsByEmail`)으로 표현 가능한 단순 쿼리만 둔다.
- 동적 쿼리를 메서드 이름으로 억지로 구현하는 것을 금지한다.
- Service가 이 인터페이스를 직접 주입받는 것을 금지하고, 항상 `{Domain}Repository`를 통해서만 접근한다.

## `{Domain}Repository` 규칙
- Service가 의존하는 유일한 진입점으로, 단순 CRUD부터 동적 조건/복잡한 조인/페이징 쿼리까지 Service에 필요한 모든 쿼리의 시그니처를 정의한다.
- `JpaRepository`, QueryDSL 등 구현 세부사항을 인터페이스에 노출하지 않는다.
- 단순 CRUD만 필요한 도메인이라도 구조 일관성을 위해 이 인터페이스를 동일하게 둔다.

## `{Domain}RepositoryImpl` 규칙
- `{Domain}Repository`를 구현하며, 클래스명 접미사를 반드시 `Impl`로 끝낸다 (Spring Data Custom Repository 규칙과의 충돌 방지, Bean 등록명 주의).
- `{Domain}JpaRepository`로 표현 가능한 단순 쿼리는 그대로 위임하고, 동적 조건이나 복잡한 쿼리만 QueryDSL 등으로 직접 구현한다.
- 비즈니스 로직(검증, 정책 판단)을 포함하지 않고 조회·저장 책임만 가진다.
- N+1 문제가 예상되는 조회는 `fetch join` 또는 `@EntityGraph`를 이 클래스에서 명시적으로 처리한다.

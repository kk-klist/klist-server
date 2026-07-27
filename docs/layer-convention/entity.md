<!-- source: https://app.notion.com/p/3a3a0d58dc6081c69be3feaf05ab8ca3 (레이어별 코딩 컨벤션 중 Entity 섹션) -->
<!-- synced: 2026-07-23 -->

# Entity 컨벤션

```java
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Builder
    private Member(String name, String email) {
        this.name = name;
        this.email = email;
        this.status = MemberStatus.ACTIVE;
    }

    public static Member create(String name, String email) {
        return Member.builder()
                .name(name)
                .email(email)
                .build();
    }

    public void changeName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new InvalidMemberNameException(newName);
        }
        this.name = newName;
    }

    public void deactivate() {
        this.status = MemberStatus.INACTIVE;
    }
}
```

## 역할 범위
- Entity는 테이블과 매핑되는 객체이면서, 자기 자신의 상태에 대한 불변식과 그 상태를 변경하는 비즈니스 규칙을 갖는 것까지 담당한다.
- DTO 변환, 영속성 조회/저장, Repository나 Service 호출은 절대 하지 않는다.
- 생성은 정적 팩토리 메서드나 빌더를 통해서만 허용하고, 상태 변경은 의미 있는 이름의 비즈니스 메서드를 통해서만 한다.

## 필드/연관관계 규칙
- Setter는 만들지 않는다.
- 생성은 `@Builder` 또는 정적 팩토리 메서드(`Member.create(...)`)로만 허용하고, public 생성자 직접 호출은 금지한다.
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 JPA 기본 생성자를 열어두되 외부 호출은 막는다.
- 연관관계의 `fetch`는 항상 `LAZY`를 기본값으로 사용하고, `EAGER`는 코드 리뷰에서 사유를 남긴 예외적인 경우에만 허용한다.
- 컬렉션 필드는 `final`로 선언하고 초기화하며, 외부에서 컬렉션 자체를 교체하는 메서드는 만들지 않는다.
- 식별자 기반 `equals`/`hashCode`는 `BaseEntity`에서 공통 처리하고 개별 Entity에서 중복 구현하는 것을 금지한다.
- DTO 변환 메서드(`toResponse()` 등)는 Entity에 두지 않고 DTO 또는 Service에 둔다.

## 연관관계 편의 메서드
- 양방향 연관관계는 꼭 필요한 경우에만 만든다.
- 연관관계 편의 메서드(`addOrder`, `setMember` 류)는 연관관계의 주인이 아닌 쪽에는 두지 않는다.

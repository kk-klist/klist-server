package com.kk.klist.domain.member.domain.entity;

import com.kk.klist.global.security.auth.Role;
import com.kk.klist.global.util.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String oauthProvider;

    @Column(nullable = false)
    private String oauthId;

    @Builder
    private Member(String nickname, String oauthProvider, String oauthId) {
        this.nickname = nickname;
        this.role = Role.USER;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
    }

    public static Member create(String nickname, String oauthProvider, String oauthId) {
        return Member.builder()
                .nickname(nickname)
                .oauthProvider(oauthProvider)
                .oauthId(oauthId)
                .build();
    }
}

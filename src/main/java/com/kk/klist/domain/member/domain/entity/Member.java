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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members", uniqueConstraints = @UniqueConstraint(columnNames = {"oauthProvider", "oauthId"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider oauthProvider;

    @Column(nullable = false)
    private String oauthId;

    @Column
    private String nationality;

    @Column
    private String preferredLanguage;

    @Column(columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean isOnboarding = false;

    @Column
    private String refreshTokenId;

    @Builder
    private Member(String nickname, OAuthProvider oauthProvider, String oauthId, String profileImageUrl) {
        this.nickname = nickname;
        this.role = Role.USER;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.profileImageUrl = profileImageUrl;
    }

    public static Member create(String nickname, OAuthProvider oauthProvider, String oauthId, String profileImageUrl) {
        return Member.builder()
                .nickname(nickname)
                .oauthProvider(oauthProvider)
                .oauthId(oauthId)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public void completeOnboarding(String nickname, String profileImageUrl, String preferredLanguage, String nationality) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.preferredLanguage = preferredLanguage;
        this.nationality = nationality;
        this.isOnboarding = true;
    }

    public void updateProfile(String nickname, String nationality) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (nationality != null) {
            this.nationality = nationality;
        }
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updatePreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public void updateRefreshToken(String tokenId) {
        this.refreshTokenId = tokenId;
    }

    public void clearRefreshToken() {
        this.refreshTokenId = null;
    }
}

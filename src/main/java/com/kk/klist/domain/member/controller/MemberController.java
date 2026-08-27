package com.kk.klist.domain.member.controller;

import com.kk.klist.domain.member.dto.request.PreferredLanguageUpdateRequest;
import com.kk.klist.domain.member.dto.request.ProfileImageUpdateRequest;
import com.kk.klist.domain.member.dto.request.ProfileUpdateRequest;
import com.kk.klist.domain.member.dto.response.MemberMeResponse;
import com.kk.klist.domain.member.dto.response.ProfileImageUpdateResponse;
import com.kk.klist.domain.member.domain.entity.Member;
import com.kk.klist.domain.member.service.MemberService;
import com.kk.klist.global.response.ApiResponse;
import com.kk.klist.global.security.auth.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> me(@LoginUser Long memberId) {
        Member member = memberService.getById(memberId);
        return ResponseEntity.ok(ApiResponse.success(MemberMeResponse.from(member)));
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateProfile(
            @LoginUser Long memberId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        memberService.updateProfile(memberId, request.nickname(), request.nationality());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<ProfileImageUpdateResponse>> updateProfileImage(
            @LoginUser Long memberId,
            @Valid @RequestBody ProfileImageUpdateRequest request) {
        String profileImageUrl = memberService.updateProfileImage(memberId, request.profileImage());
        return ResponseEntity.ok(ApiResponse.success(new ProfileImageUpdateResponse(profileImageUrl)));
    }

    @PatchMapping("/me/language")
    public ResponseEntity<Void> updatePreferredLanguage(
            @LoginUser Long memberId,
            @Valid @RequestBody PreferredLanguageUpdateRequest request) {
        memberService.updatePreferredLanguage(memberId, request.preferredLanguage());
        return ResponseEntity.noContent().build();
    }
}

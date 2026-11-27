package com.project.pawcast.outhaccount.controller;

import com.project.pawcast.outhaccount.dto.SocialAccountLinkRequest;
import com.project.pawcast.outhaccount.dto.SocialLoginRequest;
import com.project.pawcast.outhaccount.dto.SocialLoginResponse;
import com.project.pawcast.outhaccount.dto.SocialSignupRequest;
import com.project.pawcast.outhaccount.service.SocialLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class SocialLoginController {

    private final SocialLoginService socialLoginService;

    // 프론트에서 받은 토큰으로 구글 소셜 로그인(또는 자동 가입/연동 응답 반환)
    @PostMapping("/google")
    public SocialLoginResponse googleLogin(@RequestBody SocialLoginRequest request) {
        return socialLoginService.googleLogin(request.idToken());
    }

    // 추가 정보 입력을 통한 구글 소셜 회원가입 완료
    @PostMapping("/google/signup")
    public SocialLoginResponse completeGoogleSignup(@Valid @RequestBody SocialSignupRequest request) {
        return socialLoginService.completeGoogleSignup(request);
    }

    // 기존 회원 계정과 구글 소셜 계정 연동
    @PostMapping("/google/link")
    public SocialLoginResponse linkGoogleAccount(@Valid @RequestBody SocialAccountLinkRequest request) {
        return socialLoginService.linkGoogleAccount(request);
    }
}
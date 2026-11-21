package com.project.pawcast.outhaccount.controller;


import com.project.pawcast.outhaccount.dto.SocialLoginRequest;
import com.project.pawcast.outhaccount.dto.SocialLoginResponse;
import com.project.pawcast.outhaccount.dto.SocialSignupRequest;
import com.project.pawcast.outhaccount.dto.SocialAccountLinkRequest;
import com.project.pawcast.outhaccount.service.SocialLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class SocialLoginController {

    private final SocialLoginService socialLoginService;

    // 프론트에서 받은 토큰으로 구글 소셜 자동 회원 가입 및 자동 로그인
    @PostMapping("/google")
    public SocialLoginResponse googleLogin(@RequestBody SocialLoginRequest request) {
        return socialLoginService.googleLogin(request.idToken());
    }

    @PostMapping("/google/signup")
    public SocialLoginResponse completeGoogleSignup(@Valid @RequestBody SocialSignupRequest request) {
        return socialLoginService.completeGoogleSignup(request);
    }

    @PostMapping("/google/link")
    public SocialLoginResponse linkGoogleAccount(@Valid @RequestBody SocialAccountLinkRequest request) {
        return socialLoginService.linkGoogleAccount(request);
    }
}

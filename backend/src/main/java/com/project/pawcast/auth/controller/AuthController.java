package com.project.pawcast.auth.controller;

import com.project.pawcast.auth.dto.*;
import com.project.pawcast.auth.service.AuthService;
import com.project.pawcast.auth.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    // 아이디 중복 여부(버튼)
    @PostMapping("/availability")
    public Map<String, Boolean> checkUserId(@RequestParam String userid) {
        return Map.of("available", !authService.checkUserId(userid));
    }

    // 이메일 중복 확인 (GET 쿼리 파라미터 호환): 인증번호 발송 전에 가입 가능 여부를 안내한다.
    @GetMapping("/email-availability")
    public Map<String, Boolean> checkEmailByGet(
            @RequestParam
            @NotBlank(message = "빈 칸을 입력해 주세요.")
            @Email(message = "이메일 형식으로 입력해 주세요.")
            String email
    ) {
        // 기존 POST 핸들러와 동일한 검증 및 중복 조회 로직을 재사용합니다.
        return checkEmail(new EmailRequest(email));
    }

    // 이메일 중복 확인: 인증번호 발송 전에 가입 가능 여부를 명확히 안내한다.
    @PostMapping("/email-availability")
    public Map<String, Boolean> checkEmail(@Valid @RequestBody EmailRequest request) {
        return Map.of("available", !authService.checkEmail(request));
    }

    // 이메일 인증 (이메일 발송)(버튼)
    @PostMapping("/email-verify")
    public void sendVerifyEmail(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.sendVerifyEmail(request);
    }

    // 이메일 검증(이메일 확인)
    @PostMapping("/email-verify/confirm")
    public boolean checkVerifyEmail(@Valid @RequestBody EmailRequest request, @RequestParam String checkCode) {
        return authService.checkVerifyEmail(request, checkCode);
    }

    // 회원 가입
    @PostMapping("/signup")
    public SignupResponse signUp(@Valid @RequestBody SignUpRequest request) {
        authService.register(request);
        return new SignupResponse(request.name(), request.userId(), request.email());
    }

    // 로그인 (일반 로그인)
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    // 로그아웃
    @PostMapping("/logout")
    public void logout(@RequestBody LogoutRequest request) {
        authService.logout(request.refreshTokenValue());
    }

    // 비밀번호 재설정 - 버튼 누르면 임시 비밀번호 이메일 발송
    @PostMapping("/reset-password")
    public void resetPassword(@RequestParam String email) {
        authService.resetPassword(email);
    }

    // 토큰 재발급
    @PostMapping("/token/refresh")
    public TokenResponse refreshToken(
            @RequestParam(required = false) String refreshTokenValue,
            @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest request
    ) {
        // 기존 query 방식과 JSON body 방식을 모두 지원해 클라이언트 배포 시점 차이를 흡수합니다.
        String token = refreshTokenValue;
        if ((token == null || token.isBlank()) && body != null) {
            token = body.refreshTokenValue();
        }
        return authService.refresh(token, request);
    }
}
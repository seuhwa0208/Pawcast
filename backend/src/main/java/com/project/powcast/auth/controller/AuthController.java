package com.project.powcast.auth.controller;

import com.project.powcast.auth.dto.LoginRequest;
import com.project.powcast.auth.dto.LoginResponse;
import com.project.powcast.auth.dto.SignUpRequest;
import com.project.powcast.auth.dto.SignupResponse;
import com.project.powcast.auth.service.AuthService;
import com.project.powcast.auth.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    // 아이디 중복 여부(버튼)
    @PostMapping("/availability")
    public boolean checkUserId(@RequestParam String userid){
        return authService.checkUserId(userid);
    }

    // 이메일 (인증 발송)(버튼)
    @PostMapping("/email-verify")
    public void sendVerifyEmail(@RequestParam String email){
        emailVerificationService.sendVerifyEmail(email);
    }

    // 이메일 검증(이메일 확인)
    @PostMapping("/email-verify/confirm")
    public boolean checkVerifyEmail(@RequestParam String email, @RequestParam String checkCode){
        return authService.checkVerifyEmail(email, checkCode);
    }

    // 회원 가입
    @PostMapping("/signup")
    public SignupResponse signUp(@Valid @RequestBody SignUpRequest request){
        authService.register(request);
        return new SignupResponse(request.name(), request.userId(), request.email());
    }

    // 로그인 (일반 로그인)
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest){
        return authService.login(request,httpRequest);
    }

    // 소셜 로그인

    // 로그아웃

    // 비밀번호 재설정

    //



}
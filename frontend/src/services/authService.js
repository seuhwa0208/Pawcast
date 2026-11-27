// 회원가입·로그인·이메일 인증·OAuth 등 인증 관련 Spring API 호출을 한곳에 모읍니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

const post = (path, body) =>
    springRequest(path, {
        method: "POST",
        body: jsonBody(body),
    });

const postQuery = (path, params) =>
    springRequest(
        `${path}?${new URLSearchParams(params).toString()}`,
        {
            method: "POST",
        }
    );

export const authService = {
    checkAvailability: ({ userId }) =>
        postQuery("/auth/availability", {
            userid: userId,
        }),

    // 백엔드 @PostMapping("/email-availability")가 @RequestBody EmailRequest를 받으므로 JSON 바디로 전송
    checkEmailAvailability: (email) =>
        post("/auth/email-availability", {
            email,
        }),

    // 백엔드 @PostMapping("/email-verify")가 @RequestBody EmailRequest를 받으므로 JSON 바디로 전송
    sendEmailCode: (email) =>
        post("/auth/email-verify", {
            email,
        }),

    // email은 JSON 바디(EmailRequest), checkCode는 쿼리 파라미터(@RequestParam)로 전송
    confirmEmailCode: (email, verificationCode) =>
        springRequest(
            `/auth/email-verify/confirm?${new URLSearchParams({
                checkCode: verificationCode,
            }).toString()}`,
            {
                method: "POST",
                body: jsonBody({ email }),
            }
        ),

    signup: (payload) =>
        post("/auth/signup", payload),

    login: (payload) =>
        post("/auth/login", payload),

    // 로그아웃: Refresh Token을 백엔드에 전달하여 무효화
    logout: (refreshTokenValue) =>
        post("/auth/logout", {
            refreshTokenValue,
        }),

    requestPasswordReset: (email) =>
        postQuery("/auth/reset-password", {
            email,
        }),

    refresh: (refreshTokenValue) =>
        postQuery("/auth/token/refresh", {
            refreshTokenValue,
        }),

    // GIS 토큰으로 서명과 수신자 검증,
    // 액세스/리프레시 토큰 JWT 반환
    googleLogin: (idToken) =>
        post("/auth/google", {
            idToken,
        }),

    completeGoogleSignup: (payload) =>
        post("/auth/google/signup", payload),

    linkGoogleAccount: (idToken, password) =>
        post("/auth/google/link", {
            idToken,
            password,
        }),
};

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

    checkEmailAvailability: (email) =>
        postQuery("/auth/email-availability", {
            email,
        }),

    sendEmailCode: (email) =>
        postQuery("/auth/email-verify", {
            email,
        }),

    confirmEmailCode: (email, verificationCode) =>
        postQuery("/auth/email-verify/confirm", {
            email,
            checkCode: verificationCode,
        }),

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

    // Browser GIS credentials are ID tokens; Spring verifies the Google signature and audience,
    // then returns POWCAST access/refresh JWTs without a Google client secret.
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

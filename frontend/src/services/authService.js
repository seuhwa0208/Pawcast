// 회원가입·로그인·이메일 인증·OAuth 등 인증 관련 Spring API 호출을 한곳에 모읍니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

const post = (path, body) =>
  springRequest(path, { method: "POST", body: jsonBody(body) });

export const authService = {
  checkAvailability: (payload) =>
    springRequest(`/auth/availability?userid=${encodeURIComponent(payload.userId || payload.userid || "")}`, { method: "POST" }),
  sendEmailCode: (email) =>
    springRequest(`/auth/email-verify?email=${encodeURIComponent(email)}`, { method: "POST" }),
  confirmEmailCode: (email, verificationCode) =>
    springRequest(`/auth/email-verify/confirm?email=${encodeURIComponent(email)}&checkCode=${encodeURIComponent(verificationCode)}`, { method: "POST" }),
  signup: (payload) => post("/auth/signup", payload),
  login: (payload) => post("/auth/login", payload),
  requestPasswordReset: (email) =>
    springRequest(`/auth/reset-password?email=${encodeURIComponent(email)}`, {
      method: "POST",
    }),
  exchangeOAuthCode: (code) => post("/auth/oauth/exchange", { code }),
  completeGoogleSignup: (payload) => post("/auth/oauth/google/signup", payload),
  startGoogleLogin: () =>
    window.location.assign(
      `${(import.meta.env.VITE_SPRING_BASE_URL || "").replace(/\/$/, "")}/api/oauth2/authorization/google`,
    ),
};

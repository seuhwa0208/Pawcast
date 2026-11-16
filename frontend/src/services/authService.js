// 회원가입·로그인·이메일 인증·OAuth 등 인증 관련 Spring API 호출을 한곳에 모읍니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

const post = (path, body) =>
  springRequest(path, { method: "POST", body: jsonBody(body) });

export const authService = {
  checkAvailability: (payload) => post("/auth/availability", payload),
  sendEmailCode: (email) => post("/auth/email-verify", { email }),
  confirmEmailCode: (email, verificationCode) =>
    post("/auth/email-verify/confirm", { email, verificationCode }),
  signup: (payload) => post("/auth/signup", payload),
  login: (payload) => post("/auth/login", payload),
  requestPasswordReset: (email) => post("/auth/reset-password", { email }),
  resetPassword: (payload) => post("/auth/password-resets", payload),
  exchangeOAuthCode: (code) => post("/auth/oauth/exchange", { code }),
  completeGoogleSignup: (payload) => post("/auth/oauth/google/signup", payload),
  startGoogleLogin: () =>
    window.location.assign(
      `${(import.meta.env.VITE_SPRING_BASE_URL || "").replace(/\/$/, "")}/api/oauth2/authorization/google`,
    ),
};

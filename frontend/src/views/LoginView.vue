<!-- 회원 로그인과 Google OAuth 진입, 로그인 후 원래 경로 복귀를 담당합니다. -->
<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authService } from "@/services/authService";
import { useAuthSession } from "@/stores/auth";

const route = useRoute();
const router = useRouter();
const form = reactive({ userId: "", password: "" });
const loading = ref(false);
const errorMessage = ref("");
const registered = computed(() => route.query.registered === "1");
const { setAccessToken, setCurrentUserId, setUserRole } = useAuthSession();
const googleButton = ref(null);
let googleInitTimer;

async function handleGoogleCredential(response) {
  errorMessage.value = "";
  try {
    loading.value = true;
    const result = await authService.googleLogin(response.credential);
    setUserRole(result.role || "USER");
    setAccessToken(result.accessToken);
    setCurrentUserId(result.userId || result.name || String(result.memberId || "google-member"));
    router.push(typeof route.query.redirect === "string" ? route.query.redirect : "/member");
  } catch (error) {
    errorMessage.value = error.message || "Google 로그인에 실패했습니다.";
  } finally {
    loading.value = false;
  }
}

function renderGoogleButton() {
  if (!window.google?.accounts?.id || !googleButton.value) return false;
  window.google.accounts.id.initialize({
    client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
    callback: handleGoogleCredential,
  });
  window.google.accounts.id.renderButton(googleButton.value, {
    type: "standard",
    theme: "outline",
    size: "large",
    text: "continue_with",
    shape: "rectangular",
    width: Math.min(450, googleButton.value.clientWidth || 450),
  });
  return true;
}

onMounted(() => {
  if (renderGoogleButton()) return;
  googleInitTimer = window.setInterval(() => {
    if (renderGoogleButton()) window.clearInterval(googleInitTimer);
  }, 200);
});
onBeforeUnmount(() => window.clearInterval(googleInitTimer));

async function submit() {
  errorMessage.value = "";
  if (!form.userId.trim() || !form.password) {
    errorMessage.value = "아이디와 비밀번호를 모두 입력해주세요.";
    return;
  }
  try {
    loading.value = true;
    const result = await authService.login({
      userId: form.userId.trim(),
      passwordHash: form.password,
    });
    setUserRole(result.role || result.memberRole || "MEMBER");
    setAccessToken(result.accessToken);
    setCurrentUserId(form.userId.trim());
    router.push(
      typeof route.query.redirect === "string"
        ? route.query.redirect
        : result.role === "ADMIN"
          ? "/admin/dashboard"
          : "/member",
    );
  } catch (error) {
    // 차단·강퇴 계정은 전역 오류 팝업이 안내하므로 로그인 폼에 같은 문구를 중복 표시하지 않습니다.
    if (!["MEMBER_BLOCKED", "MEMBER_BANNED"].includes(error.code)) {
      errorMessage.value = error.message;
    }
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <header class="signup-heading">
        <span>WELCOME BACK</span>
        <h1>로그인</h1>
        <p>포캐스트에서 우리 아이를 위한 상품을 만나보세요.</p>
      </header>

      <form class="login-form" @submit.prevent="submit">
        <p v-if="registered" class="login-success">
          회원가입이 완료되었습니다. 로그인해주세요.
        </p>

        <label class="form-field">
          <span>아이디</span>
          <input
            v-model="form.userId"
            autocomplete="username"
            placeholder="아이디를 입력해주세요"
          />
        </label>
        <label class="form-field">
          <span>비밀번호</span>
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            placeholder="비밀번호를 입력해주세요"
          />
        </label>

        <nav class="login-tools" aria-label="계정 찾기">
          <RouterLink to="/password-reset">비밀번호 초기화 요청</RouterLink>
        </nav>

        <p v-if="errorMessage" class="form-message login-error">
          {{ errorMessage }}
        </p>
        <button class="login-submit" type="submit" :disabled="loading">
          {{ loading ? "로그인 중..." : "로그인" }}
        </button>
        <div class="auth-divider"><span>또는 간편 로그인</span></div>
        <div ref="googleButton" class="google-identity-button" aria-label="Google로 계속하기"></div>

        <div class="signup-guide">
          <div>
            <b>아직 포캐스트 회원이 아니신가요?</b
            ><span>회원가입하고 맞춤 상품을 추천받아보세요.</span>
          </div>
          <RouterLink to="/signup">회원가입</RouterLink>
        </div>
      </form>
    </section>
  </main>
</template>

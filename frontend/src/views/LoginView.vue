<!-- 일반/임시 회원 로그인과 Google OAuth 진입, 로그인 후 원래 경로 복귀를 담당합니다. -->
<script setup>
import { computed, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authService } from "@/services/authService";
import { useAuthSession } from "@/stores/auth";
import { loginTemporaryMember } from "@/data/temporaryMembers";

const route = useRoute();
const router = useRouter();
const form = reactive({ userId: "", password: "" });
const loading = ref(false);
const errorMessage = ref("");
const registered = computed(() => route.query.registered === "1");
const { setAccessToken, setCurrentUserId } = useAuthSession();
const startGoogleLogin = () => {
  sessionStorage.setItem(
    "pawcast-oauth-redirect",
    typeof route.query.redirect === "string" ? route.query.redirect : "/member",
  );
  authService.startGoogleLogin();
};

async function submit() {
  errorMessage.value = "";
  if (!form.userId.trim() || !form.password) {
    errorMessage.value = "아이디와 비밀번호를 모두 입력해주세요.";
    return;
  }
  try {
    loading.value = true;
    const temporaryResult = loginTemporaryMember(
      form.userId.trim(),
      form.password,
    );
    if (temporaryResult) {
      setAccessToken(temporaryResult.accessToken);
      setCurrentUserId(form.userId.trim());
      router.push(
        typeof route.query.redirect === "string"
          ? route.query.redirect
          : temporaryResult.member.role === "ADMIN"
            ? "/admin/dashboard"
            : "/member",
      );
      return;
    }
    const result = await authService.login({
      userId: form.userId.trim(),
      password: form.password,
    });
    setAccessToken(result.accessToken);
    setCurrentUserId(form.userId.trim());
    router.push(
      typeof route.query.redirect === "string"
        ? route.query.redirect
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
        <button
          class="google-login-button"
          type="button"
          @click="startGoogleLogin"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill="#4285F4"
              d="M21.6 12.2c0-.7-.1-1.4-.2-2H12v3.9h5.4a4.6 4.6 0 0 1-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.4Z"
            />
            <path
              fill="#34A853"
              d="M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1a5.8 5.8 0 0 1-5.4-4H3.3v2.6A10 10 0 0 0 12 22Z"
            />
            <path
              fill="#FBBC05"
              d="M6.6 14.1a6 6 0 0 1 0-4.2V7.3H3.3a10 10 0 0 0 0 9.4l3.3-2.6Z"
            />
            <path
              fill="#EA4335"
              d="M12 5.9c1.5 0 2.8.5 3.8 1.5l2.9-2.8A9.7 9.7 0 0 0 3.3 7.3l3.3 2.6A5.8 5.8 0 0 1 12 5.9Z"
            /></svg
          ><span>Google로 계속하기</span>
        </button>

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

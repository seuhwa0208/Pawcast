<!-- Google 인증 코드를 백엔드 토큰으로 교환하고 가입 필요 여부에 따라 다음 화면을 결정합니다. -->
<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { authService } from "@/services/authService";
import { useAuthSession } from "@/stores/auth";

const route = useRoute();
const router = useRouter();
const message = ref("Google 계정을 확인하고 있습니다.");
const { setAccessToken, setCurrentUserId, setPendingOAuth } = useAuthSession();

onMounted(async () => {
  const code = String(route.query.code || "");
  if (!code) {
    message.value = "Google 로그인 정보를 확인할 수 없습니다.";
    return;
  }
  try {
    const result = await authService.exchangeOAuthCode(code);
    // 백엔드 응답 버전마다 쓰는 필드명이 달라도 같은 가입 필요 상태로 해석합니다.
    const signupRequired =
      result.signupRequired ||
      result.registrationRequired ||
      result.status === "SIGNUP_REQUIRED";
    if (signupRequired) {
      setPendingOAuth({
        ticket: result.registrationToken || result.ticket,
        email: result.email || "",
        name: result.name || "",
        provider: "google",
      });
      router.replace({ path: "/signup", query: { oauth: "google" } });
      return;
    }
    setAccessToken(result.accessToken);
    setCurrentUserId(result.userId || result.email || "");
    router.replace("/member");
  } catch (error) {
    message.value = error.message;
  }
});
</script>

<template>
  <main class="login-page">
    <section class="login-card oauth-callback">
      <div class="oauth-spinner" />
      <h1>Google 로그인</h1>
      <p>{{ message }}</p>
      <RouterLink
        v-if="message !== 'Google 계정을 확인하고 있습니다.'"
        to="/login"
        >로그인으로 돌아가기</RouterLink
      >
    </section>
  </main>
</template>

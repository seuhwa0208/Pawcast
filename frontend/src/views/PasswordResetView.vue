<!-- 비밀번호 초기화 요청을 백엔드 인증 API에 접수하는 화면입니다. -->
<script setup>
import { reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { authService } from "@/services/authService";

const route = useRoute();
const form = reactive({
  email: String(route.query.email || ""),
  userId: String(route.query.userId || ""),
});
const loading = ref(false);
const requested = ref(false);
const message = ref("");

async function submitRequest() {
  message.value = "";
  if (!/^\S+@\S+\.\S+$/.test(form.email)) {
    message.value = "가입할 때 등록한 이메일을 입력해주세요.";
    return;
  }
  loading.value = true;
  try {
    await authService.requestPasswordReset(form.email.trim());
    requested.value = true;
  } catch (error) {
    message.value = error.message || "비밀번호 초기화 요청에 실패했습니다.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page recovery-page">
    <section class="login-card password-request-card">
      <header class="signup-heading">
        <span>ACCOUNT HELP</span>
        <h1>비밀번호 초기화 요청</h1>
        <p>
          관리자가 요청을 확인한 후 임시 비밀번호를 가입 이메일로 보내드립니다.
        </p>
      </header>
      <div v-if="requested" class="password-request-complete">
        <div class="modal-status-icon signup-success">
          <svg viewBox="0 0 24 24"><path d="m6 12.5 4 4L18 8" /></svg>
        </div>
        <h2>초기화 요청이 접수되었습니다</h2>
        <p>
          관리자 처리 완료 후 등록된 이메일로 임시 비밀번호가 발송됩니다.<br />처리
          전까지 기존 비밀번호는 변경되지 않습니다.
        </p>
        <RouterLink class="login-submit" to="/login"
          >로그인으로 돌아가기</RouterLink
        >
      </div>
      <form v-else class="login-form" @submit.prevent="submitRequest">
        <div class="reset-process">
          <b>처리 절차</b>
          <ol>
            <li>초기화 요청 접수</li>
            <li>관리자 확인 및 초기화</li>
            <li>임시 비밀번호 이메일 발송</li>
          </ol>
        </div>
        <label class="form-field"
          ><span>아이디 <small>(선택)</small></span
          ><input
            v-model.trim="form.userId"
            autocomplete="username"
            placeholder="아이디를 입력해주세요"
        /></label>
        <label class="form-field"
          ><span>가입 이메일</span
          ><input
            v-model.trim="form.email"
            type="email"
            autocomplete="email"
            placeholder="가입한 이메일을 입력해주세요"
        /></label>
        <p class="recovery-guide">
          계정 보호를 위해 가입 여부와 관계없이 동일한 접수 결과가 표시됩니다.
        </p>
        <p v-if="message" class="form-message recovery-message error">
          {{ message }}
        </p>
        <button class="login-submit" type="submit" :disabled="loading">
          {{ loading ? "요청 접수 중..." : "관리자에게 초기화 요청하기" }}
        </button>
        <RouterLink class="back-login" to="/login"
          >로그인으로 돌아가기</RouterLink
        >
      </form>
    </section>
  </main>
</template>

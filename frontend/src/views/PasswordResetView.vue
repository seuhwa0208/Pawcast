<!-- 비밀번호 초기화 요청을 검증하고 현재 데모용 요청 저장소에 접수하는 화면입니다. -->
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
  try {
    loading.value = true;
    await authService.requestPasswordReset(form.email.trim());
    requested.value = true;
  } catch (error) {
    message.value = error.message || "임시 비밀번호를 발급하지 못했습니다.";
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
          초기화를 누르면 임시 비밀번호가 가입 이메일로 바로 발송됩니다.
        </p>
      </header>
      <div v-if="requested" class="password-request-complete">
        <div class="modal-status-icon signup-success">
          <svg viewBox="0 0 24 24"><path d="m6 12.5 4 4L18 8" /></svg>
        </div>
        <h2>임시 비밀번호가 발송되었습니다</h2>
        <p>
          등록된 이메일에서 임시 비밀번호를 확인한 뒤 로그인해주세요.<br />로그인 후
          회원정보 설정에서 새 비밀번호로 변경할 수 있습니다.
        </p>
        <RouterLink class="login-submit" to="/login"
          >로그인으로 돌아가기</RouterLink
        >
      </div>
      <form v-else class="login-form" @submit.prevent="submitRequest">
        <div class="reset-process">
          <b>초기화 절차</b>
          <ol>
            <li>가입 이메일 입력</li>
            <li>임시 비밀번호 즉시 발급</li>
            <li>이메일 확인 후 로그인</li>
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
          {{ loading ? "임시 비밀번호 발급 중..." : "임시 비밀번호 받기" }}
        </button>
        <RouterLink class="back-login" to="/login"
          >로그인으로 돌아가기</RouterLink
        >
      </form>
    </section>
  </main>
</template>

<!-- 가입 완료 후 로그인 또는 홈 이동 선택을 부모 화면에 알리는 완료 모달입니다. -->
<script setup>
defineProps({ modelValue: Boolean });
const emit = defineEmits(["update:modelValue", "login", "home"]);
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="common-modal-backdrop">
      <section
        class="common-modal signup-complete-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="signup-complete-title"
      >
        <div class="modal-status-icon signup-success" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="m6 12.5 4 4L18 8" /></svg>
        </div>
        <h2 id="signup-complete-title">회원가입이 완료되었습니다</h2>
        <p>
          포캐스트 회원이 되신 것을 환영합니다.<br />로그인 후 회원 전용
          서비스를 이용해보세요.
        </p>
        <div class="common-modal-actions">
          <button type="button" class="primary" @click="emit('login')">
            로그인하러 가기
          </button>
          <button type="button" class="secondary" @click="emit('home')">
            메인으로 가기
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<!-- 비회원이 회원 전용 기능을 눌렀을 때 로그인 이동 여부를 묻는 공용 모달입니다. -->
<script setup>
defineProps({ modelValue: Boolean });
const emit = defineEmits(["update:modelValue", "login"]);
</script>

<template>
  <Teleport to="body"
    ><div
      v-if="modelValue"
      class="common-modal-backdrop"
      @click.self="emit('update:modelValue', false)"
    >
      <section
        class="common-modal login-required-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-required-title"
      >
        <button
          class="common-modal-close"
          type="button"
          aria-label="닫기"
          @click="emit('update:modelValue', false)"
        >
          ×
        </button>
        <div class="modal-status-icon lock" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="5" y="10" width="14" height="11" rx="2" />
            <path d="M8.5 10V7.5a3.5 3.5 0 0 1 7 0V10" />
            <circle cx="12" cy="15.5" r="1" />
            <path d="M12 16.5V18" />
          </svg>
        </div>
        <h2 id="login-required-title">로그인이 필요해요</h2>
        <p>회원 전용 기능입니다. 로그인 후 이용해 주세요.</p>
        <div class="common-modal-actions">
          <button
            type="button"
            class="secondary"
            @click="emit('update:modelValue', false)"
          >
            이전으로</button
          ><button type="button" class="primary" @click="emit('login')">
            로그인하러 가기
          </button>
        </div>
      </section>
    </div></Teleport
  >
</template>

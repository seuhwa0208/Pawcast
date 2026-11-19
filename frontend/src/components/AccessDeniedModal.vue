<!-- 비공개 문의처럼 현재 사용자에게 열람 권한이 없는 경우 표시하는 공용 모달입니다. -->
<script setup>
defineProps({ modelValue: Boolean });
const emit = defineEmits(["update:modelValue"]);
</script>

<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="common-modal-backdrop"
      @click.self="emit('update:modelValue', false)"
    >
      <section
        class="common-modal access-denied-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="access-denied-title"
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
        <h2 id="access-denied-title">열람 권한이 없습니다</h2>
        <p>
          다른 회원이 작성한 비공개 문의입니다.<br />작성자 본인만 확인할 수
          있습니다.
        </p>
        <div class="common-modal-actions single">
          <button
            type="button"
            class="primary"
            @click="emit('update:modelValue', false)"
          >
            확인
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

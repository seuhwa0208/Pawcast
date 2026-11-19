<!-- API 공통 오류를 서비스 전역에서 동일한 제목·메시지·상세 항목으로 안내합니다. -->
<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: "요청을 처리하지 못했어요" },
  message: { type: String, default: "잠시 후 다시 시도해주세요." },
  details: { type: Array, default: () => [] },
  confirmLabel: { type: String, default: "확인" },
});

const emit = defineEmits(["update:modelValue", "confirm"]);

function confirm() {
  emit("confirm");
  emit("update:modelValue", false);
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="common-modal-backdrop"
      @click.self="emit('update:modelValue', false)"
    >
      <section
        class="common-modal global-error-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="global-error-title"
      >
        <button
          type="button"
          class="common-modal-close"
          aria-label="오류 안내 닫기"
          @click="emit('update:modelValue', false)"
        >
          ×
        </button>
        <div class="modal-status-icon warning" aria-hidden="true">!</div>
        <h2 id="global-error-title">{{ title }}</h2>
        <p>{{ message }}</p>
        <ul v-if="details.length" class="global-error-details">
          <li v-for="detail in details" :key="`${detail.field}-${detail.message}`">
            {{ detail.message }}
          </li>
        </ul>
        <div class="common-modal-actions single">
          <button
            type="button"
            class="primary"
            @click="confirm"
          >
            {{ confirmLabel }}
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

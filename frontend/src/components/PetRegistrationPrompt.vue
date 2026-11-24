<!-- 반려동물이 없는 회원에게만 보이는 전역 등록 안내입니다. 숨김 기간은 브라우저에만 저장합니다. -->
<script setup>
import { ref } from "vue";

defineProps({
  modelValue: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue", "register", "defer"]);
const hideForSevenDays = ref(false);

function close() {
  if (hideForSevenDays.value) emit("defer", 7);
  emit("update:modelValue", false);
}
function dismiss() {
  hideForSevenDays.value = false;
  emit("update:modelValue", false);
}

function register() {
  emit("register");
  close();
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="common-modal-backdrop" @click.self="dismiss">
      <section
        class="common-modal pet-registration-prompt"
        role="dialog"
        aria-modal="true"
        aria-labelledby="pet-registration-title"
      >
        <button type="button" class="common-modal-close" aria-label="등록 안내 닫기" @click="dismiss">×</button>
        <div class="modal-status-icon pet" aria-hidden="true">♥</div>
        <small>PAWCAST PET PROFILE</small>
        <h2 id="pet-registration-title">반려동물을 등록하시겠어요?</h2>
        <p>등록한 반려동물 정보를 바탕으로 맞춤 상품과 AI 추천을 더 정확하게 보여드릴게요.</p>
        <label class="pet-prompt-defer">
          <input v-model="hideForSevenDays" type="checkbox" />
          <span>7일간 보지 않기</span>
        </label>
        <small class="pet-prompt-defer-note">체크 후 닫으면 7일 뒤, 다음 로그인 시 다시 안내합니다.</small>
        <div class="common-modal-actions">
          <button type="button" class="secondary" @click="close">닫기</button>
          <button type="button" class="primary" @click="register">반려동물 등록</button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.pet-registration-prompt { max-width: 480px; text-align: center; }
.pet-registration-prompt > small { color: #a46b40; font-weight: 900; font-size: 11px; letter-spacing: .1em; }
.pet-registration-prompt h2 { margin: 10px 0; color: #493328; }
.pet-registration-prompt p { margin: 0 auto 22px; max-width: 350px; color: #7e7068; line-height: 1.65; }
.modal-status-icon.pet { background: #f8e5d0; color: #9a6138; }
.pet-prompt-defer { display: flex; align-items: center; gap: 10px; margin: 0; border: 1px solid #e3c9ad; border-radius: 10px; background: #fffaf5; padding: 12px 14px; color: #805737; font-weight: 800; cursor: pointer; text-align: left; }
.pet-prompt-defer input { width: 18px; height: 18px; margin: 0; accent-color: #9a6138; cursor: pointer; }
.pet-prompt-defer-note { display: block; margin: 7px 0 16px; color: #9b7c68; font-size: 11px; text-align: left; }
</style>

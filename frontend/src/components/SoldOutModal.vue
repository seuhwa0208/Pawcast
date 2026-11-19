<!-- 품절 상품을 선택했을 때 구매 불가 이유를 안내하는 공용 모달입니다. -->
<script setup>
defineProps({ modelValue: Boolean, productName: String });
const emit = defineEmits(["update:modelValue"]);
</script>
<template>
  <Teleport to="body"
    ><div
      v-if="modelValue"
      class="common-modal-backdrop"
      @click.self="emit('update:modelValue', false)"
    >
      <section
        class="common-modal soldout-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="soldout-title"
      >
        <button
          class="common-modal-close"
          type="button"
          aria-label="닫기"
          @click="emit('update:modelValue', false)"
        >
          ×
        </button>
        <div class="modal-status-icon warning" aria-hidden="true">!</div>
        <h2 id="soldout-title">품절된 상품이에요</h2>
        <p>
          <b>{{ productName }}</b
          ><br />현재 구매할 수 없는 상품입니다. 재입고 후 다시 이용해주세요.
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
    </div></Teleport
  >
</template>

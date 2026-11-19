<!-- 장바구니 추가 결과를 알리고 계속 쇼핑/장바구니 이동을 부모에게 전달하는 모달입니다. -->
<script setup>
defineProps({ modelValue: Boolean });
const emit = defineEmits(["update:modelValue", "cart"]);
const close = () => emit("update:modelValue", false);
const goCart = () => {
  emit("update:modelValue", false);
  emit("cart");
};
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="common-modal-backdrop" @click.self="close">
      <section
        class="common-modal cart-added-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cart-added-title"
      >
        <button
          type="button"
          class="common-modal-close"
          aria-label="닫기"
          @click="close"
        >
          ×
        </button>
        <div class="cart-added-mark" aria-hidden="true">✓</div>
        <h2 id="cart-added-title">상품이 장바구니에 담겼습니다.</h2>
        <p>장바구니로 이동하시겠습니까?</p>
        <div class="common-modal-actions">
          <button type="button" class="primary" @click="goCart">
            장바구니로 가기
          </button>
          <button type="button" class="secondary" @click="close">
            계속 쇼핑하기
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

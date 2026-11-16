<!-- 상품관/카테고리 문구와 색상 변형을 일관되게 표시하는 작은 배지 컴포넌트입니다. -->
<script setup>
defineProps({
  label: { type: String, required: true },
  variant: { type: String, default: "default" },
});
</script>

<template>
  <span class="ui-tag" :class="`ui-tag--${variant}`"
    ><i aria-hidden="true" />{{ label }}</span
  >
</template>

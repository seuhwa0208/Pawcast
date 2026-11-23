<!-- # 사이드바: 공지사항·QnA 관리 - 공지 CRUD·고정 및 문의 답변 모달 -->
<script setup>
import OperationsContent from "@/views/AdminOperationsView.vue";

defineProps({
  section: {
    type: String,
    required: true,
    validator: (value) => ["notices", "qna"].includes(value),
  },
});
</script>

<template>
  <OperationsContent :section="section" />
</template>

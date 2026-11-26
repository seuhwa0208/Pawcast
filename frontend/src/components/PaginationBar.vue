<!-- 목록 화면이 공통으로 사용하는 5개 단위 페이지바: « ‹ 1 2 3 4 5 › » -->
<script setup>
import { computed } from "vue";

const props = defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  windowSize: { type: Number, default: 5 },
  label: { type: String, default: "목록 페이지" },
});
const emit = defineEmits(["update:page"]);
const safeTotal = computed(() => Math.max(1, props.totalPages));
const groupStart = computed(() => Math.floor((props.page - 1) / props.windowSize) * props.windowSize + 1);
const pages = computed(() => Array.from({ length: Math.min(props.windowSize, safeTotal.value - groupStart.value + 1) }, (_, index) => groupStart.value + index));
function move(target) { emit("update:page", Math.min(safeTotal.value, Math.max(1, target))); }
</script>

<template>
  <nav v-if="safeTotal > 1" class="pagination-bar" :aria-label="label">
    <button type="button" :disabled="page === 1" aria-label="처음 페이지" @click="move(1)">«</button>
    <button type="button" :disabled="page === 1" aria-label="이전 페이지" @click="move(page - 1)">‹</button>
    <button v-for="number in pages" :key="number" type="button" :class="{ active: page === number }" :aria-current="page === number ? 'page' : undefined" @click="move(number)">{{ number }}</button>
    <button type="button" :disabled="page === safeTotal" aria-label="다음 페이지" @click="move(page + 1)">›</button>
    <button type="button" :disabled="page === safeTotal" aria-label="마지막 페이지" @click="move(safeTotal)">»</button>
  </nav>
</template>

<style scoped>
/* 공지·Q&A·상품 리뷰·주문·관리자 목록이 모두 이 한 벌의 스타일을 씁니다. */
.pagination-bar {
  display: flex;
  width: max-content;
  max-width: 100%;
  justify-content: center;
  gap: 4px;
  margin: 22px auto 0;
  padding: 6px 12px;
  border-radius: 12px;
  background: #faf7f2;
}
.pagination-bar button {
  min-width: 38px;
  height: 38px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  padding: 0;
  color: #795e49;
  font: inherit;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition: background-color 0.16s, color 0.16s;
}
.pagination-bar button:first-child,
.pagination-bar button:nth-child(2),
.pagination-bar button:nth-last-child(2),
.pagination-bar button:last-child {
  min-width: 42px;
  font-size: 21px;
  line-height: 1;
}
.pagination-bar button:hover:not(:disabled):not(.active) {
  background: #f1e4d6;
  color: #49301f;
}
.pagination-bar button.active {
  background: #96603a;
  color: #fff;
  box-shadow: 0 3px 7px rgb(84 49 25 / 18%);
}
.pagination-bar button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
/* 좁은 화면에서도 « ‹ 1~5 › » 아홉 칸이 한 줄에 들어오도록 줄인다. */
@media (max-width: 480px) {
  .pagination-bar {
    gap: 2px;
    padding: 4px 6px;
  }
  .pagination-bar button {
    min-width: 30px;
    height: 36px;
    font-size: 14px;
  }
  .pagination-bar button:first-child,
  .pagination-bar button:nth-child(2),
  .pagination-bar button:nth-last-child(2),
  .pagination-bar button:last-child {
    min-width: 34px;
    font-size: 19px;
  }
}
</style>

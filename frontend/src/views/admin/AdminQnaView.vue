<!-- # 사이드바: QnA 관리 - 실제 문의 목록 조회·답변 등록과 5개 단위 페이지바 -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { adminManagementService } from "@/services/adminManagementService";
import PaginationBar from "@/components/PaginationBar.vue";
import "@/styles/admin-qna.css";

const qnas = ref([]);
const loading = ref(false);
const feedback = ref("");
const typeFilter = ref("ALL");
const keywordInput = ref("");
const keyword = ref("");
const selected = ref(null);
const answerContent = ref("");
const submitting = ref(false);
const page = ref(1);
const PAGE_SIZE = 10;

const typeLabel = (type) => type === "PRODUCT" ? "상품" : "일반";
const formatDate = (value) => value ? String(value).slice(0, 10).replaceAll("-", ".") : "-";
const filtered = computed(() => qnas.value.filter((item) => {
  const typeMatches = typeFilter.value === "ALL" || item.type === typeFilter.value;
  const word = keyword.value.trim().toLowerCase();
  return typeMatches && (!word || `${item.title} ${item.content}`.toLowerCase().includes(word));
}));
const pageCount = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)));
const paged = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE));
const stats = computed(() => ({ total: qnas.value.length, product: qnas.value.filter((item) => item.type === "PRODUCT").length, general: qnas.value.filter((item) => item.type === "GENERAL").length }));
function applySearch() { keyword.value = keywordInput.value; page.value = 1; }
function selectQna(item) { selected.value = item; answerContent.value = ""; feedback.value = ""; }
function closeSelection() { selected.value = null; answerContent.value = ""; }
async function loadQnas() { loading.value = true; feedback.value = ""; try { const result = await adminManagementService.getManagedQnas(); qnas.value = Array.isArray(result) ? result : []; } catch (error) { qnas.value = []; feedback.value = error.message || "QnA 목록을 불러오지 못했습니다."; } finally { loading.value = false; } }
async function submitAnswer() { if (!selected.value || !answerContent.value.trim()) { feedback.value = "답변 내용을 입력해 주세요."; return; } submitting.value = true; try { await adminManagementService.createQnaAnswer(selected.value.qnaId, answerContent.value.trim()); feedback.value = "답변을 등록했습니다."; closeSelection(); } catch (error) { feedback.value = error.message || "답변을 등록하지 못했습니다."; } finally { submitting.value = false; } }
watch([typeFilter, keyword], () => { page.value = 1; });
watch(pageCount, (count) => { if (page.value > count) page.value = count; });
onMounted(loadQnas);
</script>

<template>
  <main class="admin-qna">
    <header class="qna-heading"><small>QNA MANAGEMENT</small><h1>QnA 관리</h1><p>문의 유형을 확인하고 선택한 문의에 답변을 등록합니다.</p></header>
    <section class="qna-summary"><article><small>전체 문의</small><b>{{ stats.total }}<em>건</em></b><p>관리자 조회 기준</p></article><article><small>상품 문의</small><b>{{ stats.product }}<em>건</em></b><p>PRODUCT</p></article><article><small>일반 문의</small><b>{{ stats.general }}<em>건</em></b><p>GENERAL</p></article></section>
    <section class="qna-layout"><article class="qna-list-panel"><header><div class="qna-tabs"><button v-for="item in [{key:'ALL',label:'전체'},{key:'PRODUCT',label:'상품 문의'},{key:'GENERAL',label:'일반 문의'}]" :key="item.key" :class="{active:typeFilter===item.key}" @click="typeFilter=item.key">{{ item.label }}</button></div></header><form class="admin-qna-search" @submit.prevent="applySearch"><select v-model="typeFilter"><option value="ALL">유형 전체</option><option value="PRODUCT">상품 문의</option><option value="GENERAL">일반 문의</option></select><input v-model="keywordInput" type="search" placeholder="제목 + 내용 검색" /><button type="submit">검색하기</button></form><p v-if="feedback" class="qna-feedback">{{ feedback }}</p><p v-if="loading" class="qna-feedback">QnA 목록을 불러오는 중입니다.</p><div v-else class="qna-table-wrap"><table><thead><tr><th>유형</th><th>제목 / 내용</th><th>접수일</th><th>답변</th></tr></thead><tbody><tr v-for="item in paged" :key="item.qnaId" :class="{selected:selected?.qnaId===item.qnaId}" @click="selectQna(item)"><td><span class="qna-type" :class="item.type?.toLowerCase()">{{ typeLabel(item.type) }}</span></td><td><b>{{ item.title }}</b><small>{{ item.content }}</small></td><td>{{ formatDate(item.createdAt) }}</td><td><button type="button" @click.stop="selectQna(item)">답변</button></td></tr><tr v-if="!paged.length"><td colspan="4" class="empty">조회된 문의가 없습니다.</td></tr></tbody></table></div><PaginationBar v-model:page="page" :total-pages="pageCount" label="QnA 목록 페이지" /></article>
      <aside class="qna-answer-panel"><template v-if="selected"><header><h2>답변 작성</h2><button type="button" @click="closeSelection">닫기</button></header><div class="selected-question"><span>{{ typeLabel(selected.type) }} 문의</span><b>{{ selected.title }}</b><p>{{ selected.content }}</p></div><textarea v-model="answerContent" placeholder="답변 내용을 입력하세요" /><button class="submit-answer" :disabled="submitting" @click="submitAnswer">{{ submitting ? "등록 중" : "답변 등록" }}</button></template><p v-else>목록에서 답변할 문의를 선택해 주세요.</p></aside></section>
  </main>
</template>

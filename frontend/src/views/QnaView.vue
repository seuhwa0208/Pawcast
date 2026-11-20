<!-- Q&A는 로컬 예시를 쓰지 않고 공개·내 문의 API 결과만 표시합니다. -->
<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthSession } from "@/stores/auth";
import { qnaService } from "@/services/qnaService";
import LoginRequiredModal from "@/components/LoginRequiredModal.vue";

const route = useRoute();
const router = useRouter();
const { accessToken } = useAuthSession();
const items = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const keyword = ref("");
const type = ref("전체");
const mode = ref("list");
const selected = ref(null);
const loginRequired = ref(false);
const form = reactive({ type: "상품", title: "", content: "", isPrivate: true });
const types = ["전체", "상품", "배송", "주문/결제", "취소/반품", "회원/계정", "기타"];
const mineOnly = computed(() => route.query.scope === "mine");
const filtered = computed(() => items.value.filter((item) =>
  (type.value === "전체" || item.type === type.value) &&
  (!keyword.value || `${item.title} ${item.content}`.includes(keyword.value)),
));

function mapQna(item, index) {
  return {
    id: item.id ?? `qna-${index}-${item.title}`,
    type: item.type || "기타",
    title: item.title,
    content: item.content,
    visibility: item.isPrivate ? "PRIVATE" : "PUBLIC",
    author: item.memberName || "회원",
    status: item.answer ? "ANSWERED" : "PENDING",
    answer: item.answer?.content || "",
  };
}
async function loadQnas() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const response = mineOnly.value && accessToken.value
      ? await qnaService.myList()
      : await qnaService.list();
    items.value = (Array.isArray(response) ? response : []).map(mapQna);
  } catch (error) {
    items.value = [];
    errorMessage.value = error.message || "문의 내역을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}
function setScope(scope) {
  router.replace({ path: "/qna", query: scope === "mine" ? { scope: "mine" } : {} });
}
function openWrite() {
  if (!accessToken.value) return (loginRequired.value = true);
  mode.value = "write";
}
async function submitInquiry() {
  if (!form.title.trim() || !form.content.trim()) return;
  try {
    await qnaService.create({ ...form });
    Object.assign(form, { type: "상품", title: "", content: "", isPrivate: true });
    mode.value = "list";
    await loadQnas();
  } catch (error) {
    errorMessage.value = error.message || "문의를 등록하지 못했습니다.";
  }
}
watch(mineOnly, loadQnas);
onMounted(loadQnas);
</script>

<template>
  <main class="container qna-page">
    <header class="qna-heading">
      <div><small>PAWCAST CUSTOMER CARE</small><h1>{{ mineOnly ? "내 문의 내역" : "Q&A" }}</h1><p>상품과 주문, 배송에 관한 궁금한 내용을 남겨주세요.</p></div>
      <button v-if="mode === 'list'" class="qna-heading-write" @click="openWrite">＋ 문의하기</button>
    </header>
    <nav v-if="accessToken && mode === 'list'" class="qna-scope-tabs"><button :class="{ on: !mineOnly }" @click="setScope('all')">전체 문의</button><button :class="{ on: mineOnly }" @click="setScope('mine')">내 문의 내역</button></nav>
    <section v-if="mode === 'list'" class="qna-board">
      <div class="qna-toolbar"><select v-model="type"><option v-for="item in types" :key="item">{{ item }}</option></select><label><input v-model.trim="keyword" placeholder="제목 또는 내용을 검색하세요" /></label></div>
      <p v-if="loading" class="empty-state">문의 내역을 불러오는 중입니다.</p><p v-else-if="errorMessage" class="empty-state">{{ errorMessage }}</p>
      <div v-else class="qna-table"><div class="qna-row qna-table-head"><span>번호</span><span>제목</span><span>공개</span><span>문의 유형</span><span>작성자</span><span>상태</span></div><button v-for="(item, index) in filtered" :key="item.id" class="qna-row" @click="selected = item; mode = 'detail'"><span>{{ filtered.length - index }}</span><span class="qna-title">{{ item.visibility === 'PRIVATE' ? '🔒 ' : '' }}{{ item.title }}</span><span>{{ item.visibility === 'PUBLIC' ? '공개' : '비공개' }}</span><span>{{ item.type }}</span><span>{{ item.author }}</span><span>{{ item.status === 'ANSWERED' ? '답변완료' : '답변대기' }}</span></button><p v-if="!filtered.length" class="empty-state">등록된 문의가 없습니다.</p></div>
    </section>
    <section v-else-if="mode === 'detail'" class="qna-detail board-detail"><div class="board-detail-head"><h2>{{ selected.title }}</h2><p>{{ selected.type }} · {{ selected.visibility === 'PUBLIC' ? '공개' : '비공개' }}</p></div><article class="board-detail-content"><p>{{ selected.content }}</p></article><article v-if="selected.answer" class="qna-answer"><h3>PAWCAST 답변</h3><p>{{ selected.answer }}</p></article><p v-else class="answer-waiting">답변을 준비하고 있습니다.</p><button class="notice-list-button" @click="mode = 'list'">목록으로</button></section>
    <section v-else class="qna-write-form"><button class="back-board" @click="mode = 'list'">← 목록으로</button><h2>문의하기</h2><form @submit.prevent="submitInquiry"><label>문의 유형<select v-model="form.type"><option v-for="item in types.slice(1)" :key="item">{{ item }}</option></select></label><label>제목<input v-model.trim="form.title" maxlength="100" required /></label><label>문의 내용<textarea v-model.trim="form.content" rows="8" maxlength="2000" required /></label><label><input v-model="form.isPrivate" type="checkbox" /> 비공개 문의</label><div class="qna-write-actions"><button type="button" class="cancel" @click="mode = 'list'">취소</button><button class="submit">문의 등록</button></div></form></section>
    <LoginRequiredModal v-model="loginRequired" @login="router.push('/login')" />
  </main>
</template>

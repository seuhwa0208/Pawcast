<!-- # 사이드바: 공지사항 관리 - 공지·이벤트·고정글 CRUD와 프론트 페이지바 -->
<script setup>
import PaginationBar from "@/components/PaginationBar.vue";
import { computed, onMounted, ref, watch } from "vue";
import { noticeService } from "@/services/noticeService";
import "@/styles/admin-notices.css";

const notices = ref([]);
const loading = ref(false);
const message = ref("");
const filter = ref("ALL");
const keywordInput = ref("");
const keyword = ref("");
const page = ref(1);
const PAGE_SIZE = 10;
const editorOpen = ref(false);
const editingId = ref(null);
const form = ref({ noticeType: "GENERAL", title: "", content: "", isPinned: false });

const filtered = computed(() => notices.value.filter((notice) => {
  const typeMatch = filter.value === "ALL" || notice.noticeType === filter.value || (filter.value === "PINNED" && notice.isPinned);
  const word = keyword.value.trim().toLowerCase();
  return typeMatch && (!word || `${notice.title} ${notice.content}`.toLowerCase().includes(word));
}));
const pageCount = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)));
const paged = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE));
const stats = computed(() => ({
  total: notices.value.length,
  general: notices.value.filter((item) => item.noticeType === "GENERAL").length,
  event: notices.value.filter((item) => item.noticeType === "EVENT").length,
  pinned: notices.value.filter((item) => item.isPinned).length,
}));
const typeLabel = (type) => type === "EVENT" ? "이벤트" : "공지";
const formatDate = (value) => value ? String(value).slice(0, 10).replaceAll("-", ".") : "-";
function applySearch() { keyword.value = keywordInput.value; page.value = 1; }
watch(filter, () => { page.value = 1; });
watch(pageCount, (count) => { if (page.value > count) page.value = count; });

async function loadNotices() { loading.value = true; message.value = ""; try { const result = await noticeService.list(); notices.value = Array.isArray(result) ? result.map((item) => ({ ...item, noticeType: item.type === "이벤트" ? "EVENT" : "GENERAL", isPinned: item.pinned, createdAt: item.date })) : []; } catch (error) { message.value = error.message || "공지사항을 불러오지 못했습니다."; } finally { loading.value = false; } }
function openCreate() { editingId.value = null; form.value = { noticeType: "GENERAL", title: "", content: "", isPinned: false }; editorOpen.value = true; message.value = ""; }
async function openEdit(item) { editingId.value = item.id; form.value = { noticeType: item.noticeType, title: item.title, content: item.content, isPinned: item.isPinned }; editorOpen.value = true; message.value = ""; }
function closeEditor() { editorOpen.value = false; editingId.value = null; }
async function saveNotice() { if (!form.value.title.trim() || !form.value.content.trim()) { message.value = "제목과 내용을 입력해 주세요."; return; } try { const payload = { ...form.value, title: form.value.title.trim(), content: form.value.content.trim() }; if (editingId.value) await noticeService.update(editingId.value, payload); else await noticeService.create(payload); closeEditor(); await loadNotices(); message.value = "공지사항을 저장했습니다."; } catch (error) { message.value = error.message || "공지사항 저장에 실패했습니다."; } }
async function deleteNotice(item) { if (!window.confirm(`“${item.title}” 공지를 삭제할까요?`)) return; try { await noticeService.remove(item.id); await loadNotices(); message.value = "공지사항을 삭제했습니다."; } catch (error) { message.value = error.message || "공지사항 삭제에 실패했습니다."; } }
onMounted(loadNotices);
</script>

<template>
  <main class="admin-notices">
    <header class="notices-heading"><small>NOTICE MANAGEMENT</small><h1>{{ editorOpen ? (editingId ? "공지사항 수정" : "공지사항 등록") : "공지사항 관리" }}</h1><p>{{ editorOpen ? "공지·이벤트 유형과 상단 고정 여부를 설정합니다." : "공지와 이벤트, 상단 고정 게시글을 관리합니다." }}</p></header>
    <template v-if="editorOpen"><section class="notice-editor"><header><div><h2>{{ editingId ? "공지사항 수정" : "새 공지사항 등록" }}</h2><p>저장 즉시 공지 목록과 메인 고정 배너에 반영됩니다.</p></div><button type="button" class="outline" @click="closeEditor">목록으로</button></header><form @submit.prevent="saveNotice"><div class="form-row"><label>구분<select v-model="form.noticeType"><option value="GENERAL">공지</option><option value="EVENT">이벤트</option></select></label><label class="pin-toggle"><input v-model="form.isPinned" type="checkbox" /> 상단 고정</label></div><label>제목<input v-model="form.title" maxlength="100" placeholder="제목을 입력하세요 (최대 100자)" /></label><label>내용<textarea v-model="form.content" placeholder="공지 내용을 입력하세요" /></label><div class="form-actions"><button type="button" class="outline" @click="closeEditor">취소</button><button type="submit">{{ editingId ? "수정 저장" : "공지 등록" }}</button></div></form></section></template>
    <template v-else><section class="notice-summary"><article><small>전체 공지</small><b>{{ stats.total }}<em>건</em></b><p>삭제 제외</p></article><article><small>일반 공지</small><b>{{ stats.general }}<em>건</em></b><p>GENERAL</p></article><article><small>이벤트 공지</small><b>{{ stats.event }}<em>건</em></b><p>EVENT</p></article><article class="accent"><small>고정 공지</small><b>{{ stats.pinned }}<em>건</em></b><p>메인 배너 노출</p></article></section><section class="notice-list-panel"><header><div class="notice-tabs"><button v-for="item in [{key:'ALL',label:'전체'},{key:'GENERAL',label:'일반'},{key:'EVENT',label:'이벤트'},{key:'PINNED',label:'고정'}]" :key="item.key" :class="{active:filter===item.key}" @click="filter=item.key">{{ item.label }}</button></div><button type="button" class="primary" @click="openCreate">+ 공지 등록</button></header><form class="admin-notice-search" @submit.prevent="applySearch"><input v-model="keywordInput" type="search" placeholder="제목 · 내용 검색" /><button type="submit">검색하기</button></form><p v-if="message" class="notice-message">{{ message }}</p><p v-if="loading" class="notice-message">공지사항을 불러오는 중입니다.</p><div v-else class="notice-table-wrap"><table><thead><tr><th>번호</th><th>구분</th><th>고정</th><th>제목</th><th>등록일</th><th>관리</th></tr></thead><tbody><tr v-for="item in paged" :key="item.id"><td>{{ item.id }}</td><td><span class="notice-type" :class="item.noticeType.toLowerCase()">{{ typeLabel(item.noticeType) }}</span></td><td>{{ item.isPinned ? "고정" : "-" }}</td><td><b>{{ item.title }}</b></td><td>{{ formatDate(item.createdAt) }}</td><td><button class="small-button" @click="openEdit(item)">수정</button><button class="small-button danger" @click="deleteNotice(item)">삭제</button></td></tr><tr v-if="!paged.length"><td colspan="6" class="empty">조회된 공지사항이 없습니다.</td></tr></tbody></table></div><PaginationBar v-model:page="page" :total-pages="pageCount" label="공지사항 목록 페이지" /></section></template>
  </main>
</template>

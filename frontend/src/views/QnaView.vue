<!-- Q&A는 로컬 예시를 쓰지 않고 공개·내 문의 API 결과만 표시합니다. -->
<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
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
const keywordInput = ref("");
const type = ref("전체");
const page = ref(1);
const pageSize = 10;
const typeFilterOpen = ref(false);
const writeTypeOpen = ref(false);
const mode = ref("list");
const selected = ref(null);
const detailLoading = ref(false);
const editingId = ref(null);
const privateAccessOpen = ref(false);
const loginRequired = ref(false);
const form = reactive({
  type: "일반",
  title: "",
  content: "",
  isPrivate: true,
});
const types = ["전체", "상품", "일반"];
const inquiryTypes = types.slice(1);
const mineOnly = computed(() => route.query.scope === "mine");
const filtered = computed(() =>
  items.value.filter((item) => {
    const isPrivateForViewer = !mineOnly.value && item.visibility === "PRIVATE";
    const matchesType = type.value === "전체" || item.type === type.value;
    // 비공개 문의는 제목·내용을 검색 대상에서 제외해 정보가 노출되지 않게 한다.
    const matchesKeyword = !keyword.value || (!isPrivateForViewer
      && `${item.title} ${item.content}`.includes(keyword.value));
    return matchesType && matchesKeyword;
  }),
);
const pageCount = computed(() =>
  Math.max(1, Math.ceil(filtered.value.length / pageSize)),
);
const visibleItems = computed(() => {
  const start = (page.value - 1) * pageSize;
  return filtered.value.slice(start, start + pageSize);
});
const detailNeighbors = computed(() => {
  if (!selected.value) return { previous: null, next: null };
  const index = filtered.value.findIndex((item) => item.id === selected.value.id);
  return {
    previous: index > 0 ? filtered.value[index - 1] : null,
    next: index >= 0 && index < filtered.value.length - 1
      ? filtered.value[index + 1]
      : null,
  };
});
const pageNumbers = computed(() => {
  const start = Math.floor((page.value - 1) / 5) * 5 + 1;
  return Array.from(
    { length: Math.min(5, pageCount.value - start + 1) },
    (_, index) => start + index,
  );
});

function mapQna(item, index) {
  return {
    // # Spring QnaResponse의 식별자는 id가 아니라 qnaId다.
    id: item.qnaId ?? item.id ?? `qna-${index}-${item.title}`,
    memberId: item.memberId ?? null,
    productId: item.productId ?? null,
    type: item.type === "PRODUCT" ? "상품" : "일반",
    title: item.title,
    content: item.content,
    visibility: item.isPrivate ? "PRIVATE" : "PUBLIC",
    author: item.memberName || "회원",
    status: item.status || (item.answer ? "ANSWERED" : "PENDING"),
    answer: item.answer || "",
    createdAt: item.createdAt || item.createdDate || "",
  };
}
function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}
function selectType(value) {
  type.value = value;
  page.value = 1;
  typeFilterOpen.value = false;
}
function selectWriteType(value) {
  form.type = value;
  writeTypeOpen.value = false;
}
function applySearch() {
  keyword.value = keywordInput.value.trim();
  page.value = 1;
}
function closeSelectMenus(event) {
  if (!event.target.closest?.(".qna-type-filter")) typeFilterOpen.value = false;
  if (!event.target.closest?.(".write-type-select"))
    writeTypeOpen.value = false;
}
async function loadQnas() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const response =
      mineOnly.value && accessToken.value
        ? await qnaService.myList()
        : await qnaService.list();
    items.value = (Array.isArray(response) ? response : []).map(mapQna);
    page.value = 1;
    // 상세 URL로 새로고침해도 해당 문의를 다시 조회해 상세 화면을 복원합니다.
    if (route.params.id) await loadDetail(route.params.id);
  } catch (error) {
    items.value = [];
    errorMessage.value = error.message || "문의 내역을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}
async function loadDetail(qnaId) {
  detailLoading.value = true;
  errorMessage.value = "";
  try {
    const response = mineOnly.value
      ? await qnaService.myDetail(qnaId)
      : await qnaService.detail(qnaId);
    selected.value = mapQna(response, 0);
    mode.value = "detail";
  } catch (error) {
    selected.value = null;
    mode.value = "list";
    if (!mineOnly.value && error.status === 403) privateAccessOpen.value = true;
    else errorMessage.value = error.message || "문의 상세를 불러오지 못했습니다.";
  } finally {
    detailLoading.value = false;
  }
}
function goToList() {
  selected.value = null;
  mode.value = "list";
  router.push({ name: "qna-list", query: mineOnly.value ? { scope: "mine" } : {} });
}
function setScope(scope) {
  router.push({
    name: "qna-list",
    query: scope === "mine" ? { scope: "mine" } : {},
  });
}
function openWrite() {
  if (!accessToken.value) return (loginRequired.value = true);
  editingId.value = null;
  Object.assign(form, { type: "일반", title: "", content: "", isPrivate: true });
  mode.value = "write";
}
async function openDetail(item) {
  if (!mineOnly.value && item.visibility === "PRIVATE") {
    privateAccessOpen.value = true;
    return;
  }
  await router.push({
    name: "qna-detail",
    params: { id: item.id },
    query: mineOnly.value ? { scope: "mine" } : {},
  });
}
function openEdit() {
  if (!selected.value || !mineOnly.value) return;
  editingId.value = selected.value.id;
  Object.assign(form, {
    type: selected.value.type,
    title: selected.value.title,
    content: selected.value.content,
    isPrivate: selected.value.visibility === "PRIVATE",
  });
  mode.value = "write";
}
async function deleteInquiry() {
  if (!selected.value || !mineOnly.value) return;
  if (!window.confirm("이 문의를 삭제할까요?")) return;
  try {
    await qnaService.remove(selected.value.id);
    selected.value = null;
    await router.replace({ name: "qna-list", query: { scope: "mine" } });
    await loadQnas();
  } catch (error) {
    errorMessage.value = error.message || "문의를 삭제하지 못했습니다.";
  }
}
async function openNeighbor(item) {
  if (item) await openDetail(item);
}
async function submitInquiry() {
  if (!form.title.trim() || !form.content.trim()) return;
  try {
    if (editingId.value) await qnaService.update(editingId.value, { ...form });
    else await qnaService.create({ ...form });
    Object.assign(form, {
      type: "일반",
      title: "",
      content: "",
      isPrivate: true,
    });
    editingId.value = null;
    selected.value = null;
    await router.replace({ name: "qna-list", query: mineOnly.value ? { scope: "mine" } : {} });
    await loadQnas();
  } catch (error) {
    errorMessage.value = error.message || "문의를 등록하지 못했습니다.";
  }
}
watch(mineOnly, loadQnas);
watch(
  () => route.params.id,
  async (qnaId) => {
    if (qnaId) await loadDetail(qnaId);
    else {
      selected.value = null;
      if (mode.value === "detail") mode.value = "list";
    }
  },
);
watch(pageCount, (count) => {
  if (page.value > count) page.value = count;
});
onMounted(() => {
  document.addEventListener("pointerdown", closeSelectMenus);
  loadQnas();
});
onBeforeUnmount(() =>
  document.removeEventListener("pointerdown", closeSelectMenus),
);
</script>

<template>
  <main class="container qna-page">
    <header class="qna-heading">
      <div>
        <small>PAWCAST CUSTOMER CARE</small>
        <h1>{{ mineOnly ? "내 문의 내역" : "Q&A" }}</h1>
        <p>
          상품과 주문, 배송 등 궁금한 내용을 남겨주세요. 공개 문의는 비회원도
          확인할 수 있습니다.
        </p>
      </div>
      <button
        v-if="mode === 'list'"
        class="qna-heading-write"
        @click="openWrite"
      >
        ＋ 문의하기
      </button>
    </header>
    <nav v-if="accessToken && mode === 'list'" class="qna-scope-tabs">
      <button :class="{ on: !mineOnly }" @click="setScope('all')">
        전체 문의</button
      ><button :class="{ on: mineOnly }" @click="setScope('mine')">
        내 문의 내역
      </button>
    </nav>
    <section v-if="mode === 'list'" class="qna-board">
      <div class="qna-toolbar">
        <div class="qna-type-filter">
          <button
            type="button"
            class="qna-type-trigger"
            :class="{ open: typeFilterOpen }"
            :aria-expanded="typeFilterOpen"
            @click="typeFilterOpen = !typeFilterOpen"
          >
            <span>{{ type }}</span
            ><i aria-hidden="true" />
          </button>
          <div v-if="typeFilterOpen" class="qna-type-menu">
            <button
              v-for="item in types"
              :key="item"
              type="button"
              :class="{ selected: type === item }"
              @click="selectType(item)"
            >
              {{ item }}
            </button>
          </div>
        </div>
        <label>
          <button
            type="button"
            class="qna-search-icon"
            aria-label="검색"
            @click="applySearch"
          />
          <input
            v-model.trim="keywordInput"
            placeholder="제목 또는 내용을 검색하세요"
            @keyup.enter="applySearch"
          />
        </label>
        <button type="button" class="qna-search" @click="applySearch">
          검색
        </button>
      </div>
      <p v-if="!loading && !errorMessage" class="qna-count">
        {{ mineOnly ? "내 문의" : "전체" }} <b>{{ filtered.length }}건</b>
      </p>
      <p v-if="loading" class="empty-state">문의 내역을 불러오는 중입니다.</p>
      <p v-else-if="errorMessage" class="empty-state">{{ errorMessage }}</p>
      <div v-else class="qna-table">
        <div class="qna-row qna-table-head">
          <span>번호</span><span>제목</span><span>공개</span
          ><span>문의 유형</span><span>작성자</span><span>상태</span
          ><span>등록일</span>
        </div>
        <button
          v-for="(item, index) in visibleItems"
          :key="item.id"
          class="qna-row"
          :disabled="detailLoading"
          @click="openDetail(item)"
        >
          <span>{{ filtered.length - ((page - 1) * pageSize + index) }}</span
          ><span class="qna-title">{{ !mineOnly && item.visibility === "PRIVATE" ? "비공개 문의입니다." : item.title }}</span
          ><span>{{ item.visibility === "PUBLIC" ? "공개" : "비공개" }}</span
          ><span
            ><b
              class="qna-type-badge"
              :class="{ product: item.type === '상품' }"
              >{{ item.type }}</b
            ></span
          ><span>{{ item.author }}</span
          ><span
            ><b
              class="qna-status-badge"
              :class="{ waiting: item.status !== 'ANSWERED' }"
              >{{ item.status === "ANSWERED" ? "답변완료" : "답변대기" }}</b
            ></span
          ><span>{{ formatDate(item.createdAt) }}</span>
        </button>
        <p v-if="!filtered.length" class="empty-state">
          등록된 문의가 없습니다.
        </p>
      </div>
      <nav
        v-if="filtered.length"
        class="qna-pagination"
        aria-label="Q&A 페이지 이동"
      >
        <button :disabled="page === 1" aria-label="첫 페이지" @click="page = 1">
          «
        </button>
        <button :disabled="page === 1" aria-label="이전 페이지" @click="page--">
          ‹
        </button>
        <button
          v-for="number in pageNumbers"
          :key="number"
          :class="{ on: page === number }"
          @click="page = number"
        >
          {{ number }}
        </button>
        <button
          :disabled="page === pageCount"
          aria-label="다음 페이지"
          @click="page++"
        >
          ›
        </button>
        <button
          :disabled="page === pageCount"
          aria-label="마지막 페이지"
          @click="page = pageCount"
        >
          »
        </button>
      </nav>
    </section>
    <section v-else-if="mode === 'detail'" class="qna-detail board-detail">
      <header class="qna-detail-head">
        <small>{{ selected.type }} 문의</small>
        <h2>{{ selected.title }}</h2>
        <dl class="qna-detail-meta">
          <div>
            <dt>공개 여부</dt>
            <dd>{{ selected.visibility === "PUBLIC" ? "공개" : "비공개" }}</dd>
          </div>
          <div>
            <dt>작성자</dt>
            <dd>{{ selected.author }}</dd>
          </div>
          <div>
            <dt>문의번호</dt>
            <dd>{{ selected.id }}</dd>
          </div>
          <div>
            <dt>처리 상태</dt>
            <dd :class="{ answered: selected.status === 'ANSWERED' }">
              {{ selected.status === "ANSWERED" ? "답변 완료" : "답변 대기" }}
            </dd>
          </div>
        </dl>
      </header>
      <article class="qna-question">
        <h3>문의 내용</h3>
        <p>{{ selected.content }}</p>
      </article>
      <article v-if="selected.answer" class="qna-answer">
        <h3>PAWCAST 답변</h3>
        <p>{{ selected.answer }}</p>
      </article>
      <section v-else class="answer-waiting">
        <strong>답변을 준비하고 있습니다.</strong>
        <p>담당자가 확인한 뒤 답변을 등록합니다.</p>
      </section>
      <nav class="qna-neighbor-nav" aria-label="이전 및 다음 문의">
        <button :disabled="!detailNeighbors.previous" @click="openNeighbor(detailNeighbors.previous)">
          <small>이전글</small><span>{{ detailNeighbors.previous?.title || "이전 문의가 없습니다." }}</span>
        </button>
        <button :disabled="!detailNeighbors.next" @click="openNeighbor(detailNeighbors.next)">
          <small>다음글</small><span>{{ detailNeighbors.next?.title || "다음 문의가 없습니다." }}</span>
        </button>
      </nav>
      <footer class="qna-detail-actions">
        <div v-if="mineOnly" class="qna-owner-actions">
          <button type="button" @click="openEdit">수정</button>
          <button type="button" @click="deleteInquiry">삭제</button>
        </div>
        <button class="notice-list-button" @click="goToList">목록으로</button>
      </footer>
    </section>
    <section v-else class="qna-write-form">
      <button class="back-board" @click="mode = 'list'; editingId = null">← 목록으로</button>
      <header class="qna-write-heading">
        <small>PAWCAST CUSTOMER CARE</small>
        <h2>{{ editingId ? "문의 수정" : "문의하기" }}</h2>
        <p>문의 내용을 남겨주시면 담당자가 확인 후 답변드립니다.</p>
      </header>
      <aside class="qna-write-notice">
        <b>문의 전 확인해주세요</b>
        <span>주문·배송 관련 문의는 주문번호를 내용에 함께 적어주시면 더 빠르게 안내할 수 있어요.</span>
      </aside>
      <form @submit.prevent="submitInquiry">
        <div class="qna-form-card">
          <label class="qna-write-field"
            >문의 유형
            <div class="write-type-select">
              <button
                type="button"
                :class="{ open: writeTypeOpen }"
                @click="writeTypeOpen = !writeTypeOpen"
              >
                <span>{{ form.type }}</span
                ><i aria-hidden="true" />
              </button>
              <div v-if="writeTypeOpen" class="write-type-menu">
                <button
                  v-for="item in inquiryTypes"
                  :key="item"
                  type="button"
                  :class="{ selected: form.type === item }"
                  @click="selectWriteType(item)"
                >
                  {{ item }}
                </button>
              </div>
            </div>
          </label>
          <label class="qna-write-field"
            >제목
            <input
              v-model.trim="form.title"
              maxlength="100"
              placeholder="문의 제목을 입력해주세요"
              required
            />
            <small>{{ form.title.length }}/100</small>
          </label>
          <label class="qna-write-field qna-content-field"
            >문의 내용
            <textarea
              v-model.trim="form.content"
              rows="8"
              maxlength="2000"
              placeholder="상품, 주문, 배송과 관련해 궁금한 내용을 자세히 작성해주세요."
              required
            />
            <small>{{ form.content.length }}/2000</small>
          </label>
          <div class="visibility-selector">
            <span>공개 설정</span>
            <div>
              <label :class="{ on: !form.isPrivate }"
                ><input
                  v-model="form.isPrivate"
                  :value="false"
                  type="radio"
                /><span>공개 문의</span
                ><small>다른 고객도 확인할 수 있어요.</small></label
              >
              <label :class="{ on: form.isPrivate }"
                ><input
                  v-model="form.isPrivate"
                  :value="true"
                  type="radio"
                /><span>비공개 문의</span
                ><small>작성자와 관리자만 확인할 수 있어요.</small></label
              >
            </div>
          </div>
        </div>
        <div class="qna-write-actions">
          <button type="button" class="cancel" @click="mode = 'list'; editingId = null">
            취소</button
          ><button class="submit">{{ editingId ? "문의 수정" : "문의 등록" }}</button>
        </div>
      </form>
    </section>
    <LoginRequiredModal
      v-model="loginRequired"
      @login="router.push('/login')"
    />
    <div
      v-if="privateAccessOpen"
      class="qna-private-modal-backdrop"
      role="presentation"
      @click.self="privateAccessOpen = false"
    >
      <section class="qna-private-modal" role="alertdialog" aria-modal="true" aria-labelledby="private-qna-title">
        <button class="qna-modal-close" aria-label="닫기" @click="privateAccessOpen = false">×</button>
        <span class="qna-modal-lock" aria-hidden="true">▣</span>
        <small>PRIVATE INQUIRY</small>
        <h2 id="private-qna-title">비공개 문의입니다</h2>
        <p>비공개 문의는 작성자와 관리자만 확인할 수 있습니다.</p>
        <button class="qna-modal-confirm" @click="privateAccessOpen = false">확인</button>
      </section>
    </div>
  </main>
</template>

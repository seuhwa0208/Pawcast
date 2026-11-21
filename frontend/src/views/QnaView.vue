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
const typeFilterOpen = ref(false);
const writeTypeOpen = ref(false);
const mode = ref("list");
const selected = ref(null);
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
  items.value.filter(
    (item) =>
      (type.value === "전체" || item.type === type.value) &&
      (!keyword.value ||
        `${item.title} ${item.content}`.includes(keyword.value)),
  ),
);

function mapQna(item, index) {
  return {
    // # Spring QnaResponse의 식별자는 id가 아니라 qnaId다.
    id: item.qnaId ?? item.id ?? `qna-${index}-${item.title}`,
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
  typeFilterOpen.value = false;
}
function selectWriteType(value) {
  form.type = value;
  writeTypeOpen.value = false;
}
function applySearch() {
  keyword.value = keywordInput.value.trim();
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
  } catch (error) {
    items.value = [];
    errorMessage.value = error.message || "문의 내역을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}
function setScope(scope) {
  router.replace({
    path: "/qna",
    query: scope === "mine" ? { scope: "mine" } : {},
  });
}
function openWrite() {
  if (!accessToken.value) return (loginRequired.value = true);
  mode.value = "write";
}
async function submitInquiry() {
  if (!form.title.trim() || !form.content.trim()) return;
  try {
    await qnaService.create({ ...form });
    Object.assign(form, {
      type: "일반",
      title: "",
      content: "",
      isPrivate: true,
    });
    mode.value = "list";
    await loadQnas();
  } catch (error) {
    errorMessage.value = error.message || "문의를 등록하지 못했습니다.";
  }
}
watch(mineOnly, loadQnas);
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
          v-for="(item, index) in filtered"
          :key="item.id"
          class="qna-row"
          @click="
            selected = item;
            mode = 'detail';
          "
        >
          <span>{{ filtered.length - index }}</span
          ><span class="qna-title">{{ item.title }}</span
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
    </section>
    <section v-else-if="mode === 'detail'" class="qna-detail board-detail">
      <div class="board-detail-head">
        <h2>{{ selected.title }}</h2>
        <p>
          {{ selected.type }} ·
          {{ selected.visibility === "PUBLIC" ? "공개" : "비공개" }}
        </p>
      </div>
      <article class="board-detail-content">
        <p>{{ selected.content }}</p>
      </article>
      <article v-if="selected.answer" class="qna-answer">
        <h3>PAWCAST 답변</h3>
        <p>{{ selected.answer }}</p>
      </article>
      <p v-else class="answer-waiting">답변을 준비하고 있습니다.</p>
      <button class="notice-list-button" @click="mode = 'list'">
        목록으로
      </button>
    </section>
    <section v-else class="qna-write-form">
      <button class="back-board" @click="mode = 'list'">← 목록으로</button>
      <h2>문의하기</h2>
      <form @submit.prevent="submitInquiry">
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
          </div> </label
        ><label class="qna-write-field"
          >제목<input
            v-model.trim="form.title"
            maxlength="100"
            required /></label
        ><label class="qna-write-field"
          >문의 내용<textarea
            v-model.trim="form.content"
            rows="8"
            maxlength="2000"
            required
          />
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
        <div class="qna-write-actions">
          <button type="button" class="cancel" @click="mode = 'list'">
            취소</button
          ><button class="submit">문의 등록</button>
        </div>
      </form>
    </section>
    <LoginRequiredModal
      v-model="loginRequired"
      @login="router.push('/login')"
    />
  </main>
</template>

<!-- 공개/비공개 문의 권한, 검색·필터, 내 문의 로컬 저장을 함께 처리하는 Q&A 화면입니다. -->
<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { inquiries } from "@/data/inquiries";
import { useAuthSession } from "@/stores/auth";
import LoginRequiredModal from "@/components/LoginRequiredModal.vue";
import AccessDeniedModal from "@/components/AccessDeniedModal.vue";

const router = useRouter();
const route = useRoute();
const { accessToken, currentUserId } = useAuthSession();
const effectiveUserId = computed(
  () => currentUserId.value,
);
// 사용자별 저장 키로 문의 내역이 다른 계정에 노출되는 것을 방지합니다.
const inquiryStorageKey = `powcast-my-inquiries-v2:${effectiveUserId.value || "guest"}`;
const sampleMyInquiries = [
  {
    id: 205,
    type: "배송",
    title: "배송지를 변경할 수 있을까요?",
    content: "주문한 상품의 배송지를 회사 주소로 변경하고 싶습니다.",
    visibility: "PRIVATE",
    author: "나",
    status: "ANSWERED",
    date: "2026.11.11",
    answer:
      "결제 완료 상태에서는 주문 상세 화면에서 배송지를 변경할 수 있습니다.",
  },
  {
    id: 204,
    type: "상품",
    title: "알레르기 성분을 확인하고 싶어요",
    content: "연어 간식에 닭고기 유래 성분이 포함되어 있는지 궁금합니다.",
    visibility: "PUBLIC",
    author: "나",
    status: "ANSWERED",
    date: "2026.11.11",
    answer:
      "상품 상세의 원재료 정보에서 확인할 수 있으며, 해당 상품에는 닭고기 유래 성분이 포함되어 있지 않습니다.",
  },
  {
    id: 203,
    type: "주문/결제",
    title: "적립금은 언제 지급되나요?",
    content: "구매 확정을 완료했는데 적립금 지급 시점이 궁금합니다.",
    visibility: "PRIVATE",
    author: "나",
    status: "PENDING",
    date: "2026.11.10",
    answer: "",
  },
  {
    id: 202,
    type: "취소/반품",
    title: "포장을 개봉하지 않은 상품 반품 문의",
    content:
      "배송받은 상품을 아직 개봉하지 않았습니다. 반품 절차를 알려주세요.",
    visibility: "PRIVATE",
    author: "나",
    status: "ANSWERED",
    date: "2026.11.09",
    answer:
      "주문 상세에서 반품을 신청해 주세요. 회수 확인 후 환불이 진행됩니다.",
  },
  {
    id: 201,
    type: "회원/계정",
    title: "마케팅 수신 설정 변경 문의",
    content: "마케팅 수신 설정을 다시 변경하고 싶습니다.",
    visibility: "PUBLIC",
    author: "나",
    status: "PENDING",
    date: "2026.11.08",
    answer: "",
  },
].map((item) => ({ ...item, ownerId: effectiveUserId.value }));
const myInquiries = ref(
  JSON.parse(localStorage.getItem(inquiryStorageKey) || "null") ||
    sampleMyInquiries,
);
if (!localStorage.getItem(inquiryStorageKey))
  localStorage.setItem(inquiryStorageKey, JSON.stringify(myInquiries.value));
const allInquiries = computed(() => [...myInquiries.value, ...inquiries]);
const mineOnly = computed(() => route.query.scope === "mine");
const type = ref("전체"),
  typeOpen = ref(false),
  writeTypeOpen = ref(false),
  keyword = ref(""),
  page = ref(1),
  mode = ref("list"),
  selected = ref(null),
  loginRequired = ref(false),
  permissionDenied = ref(false);
const form = ref({
  type: "상품",
  title: "",
  content: "",
  visibility: "PRIVATE",
});
const types = [
  "전체",
  "상품",
  "배송",
  "주문/결제",
  "취소/반품",
  "회원/계정",
  "기타",
];
const filtered = computed(() =>
  allInquiries.value
    .filter(
      (item) =>
        (!mineOnly.value || item.ownerId === effectiveUserId.value) &&
        (type.value === "전체" || item.type === type.value) &&
        (!keyword.value ||
          `${item.title} ${item.content}`.includes(keyword.value)),
    )
    .sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id),
);
const pageCount = computed(() =>
  Math.max(1, Math.ceil(filtered.value.length / 10)),
);
const visibleItems = computed(() =>
  filtered.value.slice((page.value - 1) * 10, page.value * 10),
);
const pageNumbers = computed(() => {
  const start = Math.floor((page.value - 1) / 5) * 5 + 1;
  return Array.from(
    { length: Math.min(5, pageCount.value - start + 1) },
    (_, index) => start + index,
  );
});
function requireLogin(action) {
  if (!accessToken.value) {
    loginRequired.value = true;
    return;
  }
  action();
}
function openDetail(item) {
  if (item.visibility === "PRIVATE" && item.ownerId !== effectiveUserId.value) {
    permissionDenied.value = true;
    return;
  }
  selected.value = item;
  mode.value = "detail";
}
function submitInquiry() {
  if (!form.value.title.trim() || !form.value.content.trim()) return;
  myInquiries.value.unshift({
    id: Date.now(),
    ...form.value,
    ownerId: effectiveUserId.value,
    author: "나",
    status: "PENDING",
    date: new Date().toISOString().slice(0, 10),
    answer: "",
  });
  localStorage.setItem(inquiryStorageKey, JSON.stringify(myInquiries.value));
  mode.value = "list";
  form.value = { type: "상품", title: "", content: "", visibility: "PRIVATE" };
}
function goToLogin() {
  loginRequired.value = false;
  router.push("/login");
}
function chooseType(value) {
  type.value = value;
  typeOpen.value = false;
  page.value = 1;
}
function chooseWriteType(value) {
  form.value.type = value;
  writeTypeOpen.value = false;
}
function setScope(scope) {
  router.replace({
    path: "/qna",
    query: scope === "mine" ? { scope: "mine" } : {},
  });
  page.value = 1;
  mode.value = "list";
}
</script>

<template>
  <main class="container qna-page">
    <header class="qna-heading">
      <div>
        <small>POWCAST CUSTOMER CARE</small>
        <h1>{{ mineOnly ? "내 문의 내역" : "Q&A" }}</h1>
        <p>
          {{
            mineOnly
              ? "내가 작성한 공개·비공개 문의와 답변 상태를 확인할 수 있습니다."
              : "상품과 주문, 배송 등 궁금한 내용을 남겨주세요. 공개 문의는 비회원도 확인할 수 있습니다."
          }}
        </p>
      </div>
      <button
        v-if="mode === 'list'"
        class="qna-heading-write"
        @click="requireLogin(() => (mode = 'write'))"
      >
        ＋ 문의하기
      </button>
    </header>
    <nav
      v-if="accessToken && mode === 'list'"
      class="qna-scope-tabs"
      aria-label="문의 목록 범위"
    >
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
            :class="{ open: typeOpen }"
            :aria-expanded="typeOpen"
            @click="typeOpen = !typeOpen"
          >
            <span>{{ type }}</span
            ><i aria-hidden="true" />
          </button>
          <div v-if="typeOpen" class="qna-type-menu">
            <button
              v-for="item in types"
              :key="item"
              type="button"
              :class="{ selected: type === item }"
              @click="chooseType(item)"
            >
              <span>{{ item }}</span
              ><b v-if="type === item">✓</b>
            </button>
          </div>
        </div>
        <label
          ><input
            v-model.trim="keyword"
            placeholder="제목 또는 내용을 검색하세요" /><span
            class="qna-search-icon"
            aria-hidden="true" /></label
        ><button class="qna-search" @click="page = 1">검색</button>
      </div>
      <div class="qna-count">
        전체 <b>{{ filtered.length }}</b
        >건
      </div>
      <div class="qna-table">
        <div class="qna-row qna-table-head">
          <span>번호</span><span>제목</span><span>공개</span
          ><span>문의 유형</span><span>작성자</span><span>상태</span
          ><span>등록일</span>
        </div>
        <button
          v-for="item in visibleItems"
          :key="item.id"
          class="qna-row"
          :class="{ private: item.visibility === 'PRIVATE' }"
          @click="openDetail(item)"
        >
          <span>{{ item.id }}</span
          ><span class="qna-title"
            ><i v-if="item.visibility === 'PRIVATE'">🔒</i
            >{{ item.title }}</span
          ><span>{{ item.visibility === "PUBLIC" ? "공개" : "비공개" }}</span
          ><span
            ><b class="type-badge">{{ item.type }}</b></span
          ><span>{{ item.author }}</span
          ><span
            ><b class="status-badge" :class="item.status.toLowerCase()">{{
              item.status === "ANSWERED" ? "답변완료" : "답변대기"
            }}</b></span
          ><span>{{ item.date }}</span>
        </button>
      </div>
      <nav class="qna-pagination" aria-label="페이지 이동">
        <button :disabled="page === 1" aria-label="첫 페이지" @click="page = 1">
          «</button
        ><button
          :disabled="page === 1"
          aria-label="이전 페이지"
          @click="page--"
        >
          ‹</button
        ><button
          v-for="n in pageNumbers"
          :key="n"
          :class="{ on: page === n }"
          @click="page = n"
        >
          {{ n }}</button
        ><button
          :disabled="page === pageCount"
          aria-label="다음 페이지"
          @click="page++"
        >
          ›</button
        ><button
          :disabled="page === pageCount"
          aria-label="마지막 페이지"
          @click="page = pageCount"
        >
          »
        </button>
      </nav>
    </section>
    <section v-else-if="mode === 'detail'" class="qna-detail board-detail">
      <div class="qna-detail-head board-detail-head">
        <div class="qna-detail-title">
          <span class="type-badge">{{ selected.type }}</span>
          <h2>{{ selected.title }}</h2>
        </div>
        <dl class="qna-detail-meta">
          <div>
            <dt>작성자</dt>
            <dd>{{ selected.author }}</dd>
          </div>
          <div>
            <dt>등록일</dt>
            <dd>{{ selected.date }}</dd>
          </div>
          <div>
            <dt>공개 여부</dt>
            <dd>{{ selected.visibility === "PUBLIC" ? "공개" : "비공개" }}</dd>
          </div>
        </dl>
      </div>
      <article class="qna-question board-detail-content">
        <h3>문의 내용</h3>
        <p>{{ selected.content }}</p>
      </article>
      <article v-if="selected.answer" class="qna-answer">
        <h3>POWCAST 답변</h3>
        <p>{{ selected.answer }}</p>
      </article>
      <div v-else class="answer-waiting">답변을 준비하고 있습니다.</div>
      <div class="board-detail-actions">
        <button class="notice-list-button" @click="mode = 'list'">
          목록으로
        </button>
      </div>
    </section>
    <section v-else class="qna-write-form">
      <button class="back-board" @click="mode = 'list'">← 목록으로</button>
      <div class="qna-write-heading">
        <small>POWCAST CUSTOMER CARE</small>
        <h2>문의하기</h2>
        <p>궁금한 내용을 남겨주시면 확인 후 정성껏 답변드리겠습니다.</p>
      </div>
      <aside class="qna-write-notice">
        <b>작성 전 확인해주세요</b
        ><span>주문번호·연락처 등 개인정보는 공개 문의에 작성하지 마세요.</span>
      </aside>
      <form @submit.prevent="submitInquiry">
        <div class="qna-form-card">
          <label class="qna-write-field"
            ><span>문의 유형</span>
            <div class="write-type-select">
              <button
                type="button"
                :class="{ open: writeTypeOpen }"
                @click="writeTypeOpen = !writeTypeOpen"
              >
                <span>{{ form.type }}</span
                ><i />
              </button>
              <div v-if="writeTypeOpen" class="write-type-menu">
                <button
                  v-for="item in types.slice(1)"
                  :key="item"
                  type="button"
                  :class="{ selected: form.type === item }"
                  @click="chooseWriteType(item)"
                >
                  <span>{{ item }}</span
                  ><b v-if="form.type === item">✓</b>
                </button>
              </div>
            </div></label
          >
          <label class="qna-write-field"
            ><span>제목</span
            ><input
              v-model="form.title"
              maxlength="100"
              placeholder="문의 제목을 입력해주세요"
            /><small>{{ form.title.length }} / 100</small></label
          >
          <fieldset class="visibility-selector">
            <legend>공개 여부</legend>
            <div>
              <label :class="{ on: form.visibility === 'PRIVATE' }"
                ><input
                  v-model="form.visibility"
                  value="PRIVATE"
                  type="radio"
                /><span>비공개</span><small>나와 관리자만 확인</small></label
              ><label :class="{ on: form.visibility === 'PUBLIC' }"
                ><input
                  v-model="form.visibility"
                  value="PUBLIC"
                  type="radio"
                /><span>공개</span><small>모든 사용자가 확인</small></label
              >
            </div>
          </fieldset>
          <label class="qna-write-field qna-content-field"
            ><span>문의 내용</span
            ><textarea
              v-model="form.content"
              rows="8"
              maxlength="2000"
              placeholder="문의 내용을 자세히 입력해주세요"
            /><small>{{ form.content.length }} / 2,000</small></label
          >
        </div>
        <div class="qna-write-actions">
          <button type="button" class="cancel" @click="mode = 'list'">
            취소</button
          ><button type="submit" class="submit">문의 등록</button>
        </div>
      </form>
    </section>
    <LoginRequiredModal v-model="loginRequired" @login="goToLogin" />
    <AccessDeniedModal v-model="permissionDenied" />
  </main>
</template>

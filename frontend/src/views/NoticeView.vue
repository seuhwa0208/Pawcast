<!-- 공지/이벤트의 검색·고정 글 분리·페이지네이션·상세 열람을 처리하는 게시판입니다. -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { noticeService } from "@/services/noticeService";
const route = useRoute(),
  router = useRouter();
const notices = ref([]);
const loadError = ref("");
const filter = ref("전체"),
  keyword = ref(""),
  keywordInput = ref(""),
  selected = ref(null),
  page = ref(1);
onMounted(async () => {
  try {
    loadError.value = "";
    notices.value = await noticeService.list();
    await selectFromRoute(route.params.id);
  } catch (error) {
    notices.value = [];
    loadError.value = error.message || "공지사항을 불러오지 못했습니다.";
  }
});
async function selectFromRoute(noticeId) {
  if (!noticeId) {
    selected.value = null;
    return;
  }
  try {
    loadError.value = "";
    selected.value = await noticeService.detail(noticeId);
  } catch (error) {
    selected.value = null;
    loadError.value = error.message || "공지사항 상세를 불러오지 못했습니다.";
  }
}
function openNotice(item) {
  router.push({ name: "notice-detail", params: { id: item.id } });
}
const filtered = computed(() =>
  notices.value
    .filter(
      (item) =>
        (filter.value === "전체" || item.type === filter.value) &&
        (!keyword.value ||
          `${item.title} ${item.content}`.includes(keyword.value)),
    )
    .sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id),
);
const pinned = computed(() => filtered.value.filter((item) => item.pinned)),
  normal = computed(() => filtered.value.filter((item) => !item.pinned));
const noticeCountLabel = computed(() =>
  filter.value === "전체" ? "전체" : filter.value,
);
const pageCount = computed(() =>
    Math.max(1, Math.ceil(normal.value.length / 10)),
  ),
  visibleItems = computed(() =>
    normal.value.slice((page.value - 1) * 10, page.value * 10),
  );
// 현재 페이지가 속한 5개 단위 페이지 묶음만 화면에 노출합니다.
const pageNumbers = computed(() => {
  const start = Math.floor((page.value - 1) / 5) * 5 + 1;
  return Array.from(
    { length: Math.min(5, pageCount.value - start + 1) },
    (_, index) => start + index,
  );
});
function chooseFilter(value) {
  filter.value = value;
  page.value = 1;
}
function applySearch() {
  keyword.value = keywordInput.value.trim();
  page.value = 1;
}
function closeDetail() {
  selected.value = null;
  router.push({ name: "notice-list" });
}
watch(() => route.params.id, (noticeId) => selectFromRoute(noticeId));
</script>
<template>
  <main class="container notice-page">
    <header class="notice-heading">
      <small>POWCAST NEWS</small>
      <h1>공지 / 이벤트</h1>
      <p>포캐스트의 새로운 소식과 다양한 혜택을 확인해보세요.</p>
    </header>
    <section v-if="!selected" class="notice-board">
      <div class="notice-toolbar">
        <div class="notice-filters">
          <button
            v-for="item in ['전체', '공지', '이벤트']"
            :key="item"
            :class="{ on: filter === item }"
            @click="chooseFilter(item)"
          >
            {{ item }}
          </button>
        </div>
        <label>
          <button type="button" class="notice-search-icon" aria-label="검색" @click="applySearch" />
          <input
            v-model.trim="keywordInput"
            placeholder="예) 배송 문의"
            @keyup.enter="applySearch"
          />
        </label>
      </div>
      <p v-if="!loadError" class="notice-count">
        {{ noticeCountLabel }} <b>{{ filtered.length }}건</b>
      </p>
      <p v-if="loadError" class="empty-data-notice">
        {{ loadError }} 서버 연결 상태를 확인해주세요.
      </p>
      <div v-if="pinned.length" class="pinned-notices">
        <button v-for="item in pinned" :key="item.id" @click="openNotice(item)">
          <span class="pin-mark">고정</span>
          <span
            class="notice-type"
            :class="{ event: item.type === '이벤트' }"
            >{{ item.type }}</span
          ><strong>{{ item.title }}</strong
          ><time>{{ item.date }}</time>
        </button>
      </div>
      <div class="notice-list" :class="{ 'without-pinned': !pinned.length }">
        <div class="notice-list-head">
          <span>번호</span><span>구분</span><span>제목</span><span>등록일</span>
        </div>
        <button
          v-for="item in visibleItems"
          :key="item.id"
          @click="openNotice(item)"
        >
          <span>{{ item.id }}</span
          ><span
            ><b
              class="notice-type"
              :class="{ event: item.type === '이벤트' }"
              >{{ item.type }}</b
            ></span
          ><span class="notice-title">{{ item.title }}</span
          ><span>{{ item.date }}</span>
        </button>
      </div>
      <p v-if="!pinned.length && !visibleItems.length && !loadError" class="board-empty-state">
        검색 조건에 맞는 게시글이 없습니다.
      </p>
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
    <section v-else class="notice-detail board-detail">
      <header class="board-detail-head">
        <div>
          <span
            class="notice-type"
            :class="{ event: selected.type === '이벤트' }"
            >{{ selected.type }}</span
          ><span v-if="selected.pinned" class="pin-mark">고정</span>
        </div>
        <h2>{{ selected.title }}</h2>
        <p>
          <time>{{ selected.date }}</time>
        </p>
      </header>
      <article class="board-detail-content">
        <p>{{ selected.content }}</p>
      </article>
      <div class="board-detail-actions">
        <button class="notice-list-button" @click="closeDetail">
          목록으로
        </button>
      </div>
    </section>
  </main>
</template>

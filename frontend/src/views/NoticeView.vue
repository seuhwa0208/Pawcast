<!-- 공지/이벤트의 검색·고정 글 분리·페이지네이션·상세 열람을 처리하는 게시판입니다. -->
<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { noticeService } from "@/services/noticeService";
const route = useRoute(),
  router = useRouter();
const notices = ref([]);
const filter = ref("전체"),
  keyword = ref(""),
  selected = ref(null),
  page = ref(1);
onMounted(async () => {
  try {
    notices.value = await noticeService.list();
    selected.value = notices.value.find((item) => item.id === Number(route.query.id)) || null;
  } catch {
    notices.value = [];
  }
});
const filtered = computed(() =>
  notices
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
function closeDetail() {
  selected.value = null;
  if (route.query.id) router.replace("/notices");
}
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
        <label
          ><input
            v-model.trim="keyword"
            placeholder="제목 또는 내용을 검색하세요"
          /><span>⌕</span></label
        ><button class="notice-search">검색</button>
      </div>
      <div class="pinned-notices">
        <button v-for="item in pinned" :key="item.id" @click="selected = item">
          <span class="pin-mark">고정</span
          ><span
            class="notice-type"
            :class="{ event: item.type === '이벤트' }"
            >{{ item.type }}</span
          ><strong>{{ item.title }}</strong
          ><time>{{ item.date }}</time
          ><span class="event-state"
            ><em
              v-if="item.status"
              :class="{ ended: item.status === '종료' }"
              >{{ item.status }}</em
            ><i v-else>—</i></span
          >
        </button>
      </div>
      <div class="notice-list">
        <div class="notice-list-head">
          <span>번호</span><span>구분</span><span>제목</span><span>등록일</span
          ><span>이벤트 상태</span><span>조회</span>
        </div>
        <button
          v-for="item in visibleItems"
          :key="item.id"
          @click="selected = item"
        >
          <span>{{ item.id }}</span
          ><span
            ><b
              class="notice-type"
              :class="{ event: item.type === '이벤트' }"
              >{{ item.type }}</b
            ></span
          ><span class="notice-title">{{ item.title }}</span
          ><span>{{ item.date }}</span
          ><span class="event-state"
            ><em
              v-if="item.status"
              :class="{ ended: item.status === '종료' }"
              >{{ item.status }}</em
            ><i v-else>—</i></span
          ><span>{{ item.views.toLocaleString() }}</span>
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
          <time>{{ selected.date }}</time
          ><span>조회 {{ selected.views.toLocaleString() }}</span>
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

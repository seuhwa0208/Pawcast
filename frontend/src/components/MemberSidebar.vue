<!-- 마이페이지 계열 화면이 공유하는 회원 정보 요약과 탐색 메뉴입니다. -->
<script setup>
defineProps({
  name: { type: String, default: "포캐스트 회원" },
  userId: { type: String, default: "" },
  active: { type: String, default: "" },
});
const emit = defineEmits(["settings"]);
</script>

<template>
  <aside class="member-sidebar">
    <div class="member-identity">
      <b>{{ name }} 님</b><span v-if="userId">{{ userId }}</span
      ><small>일반회원</small>
    </div>
    <nav>
      <strong>나의 주문 정보</strong>
      <RouterLink to="/mypage#orders" :class="{ on: active === 'orders' }"
        >주문·배송 조회</RouterLink
      >
      <RouterLink to="/mypage/claims" :class="{ on: active === 'claims' }"
        >취소·반품·교환</RouterLink
      >
      <strong>나의 활동</strong>
      <RouterLink
        to="/mypage/consumption"
        :class="{ on: active === 'consumption' }"
        >소진일 관리</RouterLink
      >
      <RouterLink to="/mypage/pets" :class="{ on: active === 'pets' }"
        >반려동물 정보</RouterLink
      >
      <strong>회원 관리</strong>
      <button
        type="button"
        :class="{ on: active === 'settings' }"
        @click="emit('settings')"
      >
        회원정보 수정
      </button>
      <RouterLink :to="{ path: '/qna', query: { scope: 'mine' } }"
        >내 문의 내역</RouterLink
      >
      <RouterLink to="/notices">공지사항</RouterLink>
    </nav>
  </aside>
</template>

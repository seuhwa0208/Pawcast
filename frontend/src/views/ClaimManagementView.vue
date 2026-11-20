<!-- 취소와 환불은 현재 백엔드 API가 제공하는 범위에서만 안내·조회합니다. -->
<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import MemberSidebar from "@/components/MemberSidebar.vue";
import { useAuthSession } from "@/stores/auth";

const router = useRouter();
const { currentUserId } = useAuthSession();
const displayName = computed(() => currentUserId.value || "포캐스트 회원");
</script>

<template>
  <main class="container member-dashboard claim-management-page">
    <header class="dashboard-heading">
      <span>POWCAST ORDER CARE</span>
      <h1>취소·반품(환불)</h1>
      <p>주문 취소와 환불 처리 정보를 확인하세요.</p>
    </header>
    <div class="dashboard-layout">
      <MemberSidebar :name="displayName" :user-id="currentUserId" active="claims" @settings="router.push('/mypage?verify=1')" />
      <section class="dashboard-content dashboard-order-state">
        <h2>취소·환불 안내</h2>
        <p>결제 완료 주문은 주문 상세에서 바로 취소할 수 있습니다. 환불 내역은 환불 처리 후 표시됩니다.</p>
      </section>
    </div>
  </main>
</template>

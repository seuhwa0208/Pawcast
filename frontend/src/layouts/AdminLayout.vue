
<script setup>
import AdminSidebar from "@/components/AdminSidebar.vue";
</script>

<template>
  <section class="admin-layout">
    <!-- 관리자 공통 사이드바 -->
    <AdminSidebar />

    <!-- 선택한 관리자 메뉴의 화면 -->
    <div class="admin-layout-content">
      <RouterView />
    </div>
  </section>
</template>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  min-height: 100dvh;
  background: #faf7f2;
}

.admin-layout-content {
  min-width: 0;
}

@media (max-width: 760px) {
  .admin-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

<script setup>
import { useRouter } from "vue-router";
import { useAuthSession } from "@/stores/auth";
import { isAdminRole, readUserRole } from "@/utils/roles";

const router = useRouter();
const { clearAuthSession } = useAuthSession();
const isAdmin = isAdminRole(readUserRole());

const menu = [
  ...(isAdmin ? [{ label: "매출 분석", to: "/admin/sales" }] : []),
  { label: "주문·배송 관리", to: "/admin/orders" },
  { label: "상품 관리", to: "/admin/products" },
  { label: "상품 등록", to: "/admin/products/new" },
  ...(isAdmin ? [{ label: "회원 관리", to: "/admin/members" }] : []),
  { label: "공지사항 관리", to: "/admin/notices" },
  { label: "QnA 관리", to: "/admin/qna" },
];

function logout() {
  // Access Token과 회전 가능한 Refresh Token을 함께 지워 재로그인을 요구합니다.
  clearAuthSession();
  router.push("/login");
}
</script>

<template>
  <aside class="admin-nav-sidebar">
    <header>
      <b>PAWCAST</b>
      <span>{{ isAdmin ? "관리자 페이지" : "매니저 페이지" }}</span>
    </header>
    <nav aria-label="관리자 메뉴">
      <RouterLink v-for="item in menu" :key="item.to" :to="item.to">
        {{ item.label }}
      </RouterLink>
    </nav>
    <button type="button" class="admin-nav-logout" @click="logout">로그아웃</button>
  </aside>
</template>

<style scoped>
.admin-nav-sidebar {
  position: sticky;
  top: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 260px;
  height: 100dvh;
  min-height: 620px;
  padding: 30px 18px 18px;
  overflow-y: auto;
  border-right: 1px solid #eadfd3;
  background: #fffcf7;
}
.admin-nav-sidebar header { padding: 0 10px 22px; border-bottom: 1px solid #eadfd3; }
.admin-nav-sidebar header b { display: block; color: #875738; font-size: 26px; letter-spacing: -0.04em; }
.admin-nav-sidebar header span { color: #987e6b; font-size: 12px; }
.admin-nav-sidebar nav { display: grid; gap: 6px; margin-top: 22px; }
.admin-nav-sidebar a,
.admin-nav-logout {
  display: flex;
  align-items: center;
  min-height: 46px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  padding: 0 14px;
  color: #4b3425;
  font: inherit;
  font-weight: 800;
  text-decoration: none;
  text-align: left;
  cursor: pointer;
}
.admin-nav-sidebar a:hover,
.admin-nav-sidebar a.router-link-exact-active { background: #a66b3f; color: #fff; }
.admin-nav-logout { flex: 0 0 auto; justify-content: center; margin-top: auto; border: 1px solid #d8bca2; background: #fff; }
.admin-nav-logout:hover { border-color: #a66b3f; background: #fff7f0; }
@media (max-width: 760px) {
  .admin-nav-sidebar { position: static; width: 100%; height: auto; min-height: 0; padding: 20px 14px; }
  .admin-nav-sidebar nav { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .admin-nav-sidebar a { min-height: 42px; padding: 0 10px; font-size: 13px; }
  .admin-nav-logout { min-height: 42px; margin-top: 16px; }
}
</style>



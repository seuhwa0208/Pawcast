<!-- 관리자 API 상태를 한눈에 보여주고, 실패 시에는 명시적으로 대체 상태를 표시합니다. -->
<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthSession } from "@/stores/auth";
import { adminManagementService } from "@/services/adminManagementService";

const router = useRouter();
const { setAccessToken } = useAuthSession();
const loading = ref(true);
const apiIssue = ref(false);
const refreshedAt = ref("");
const stats = ref({ orders: 0, preparing: 0, products: 0, soldOut: 0, members: 0, notices: 0, qnas: 0 });
const menu = computed(() => [
  { path: "/admin/sales", title: "매출 분석", value: "일·월·분기·연도", text: "기간과 카테고리 기준 매출을 조회합니다." },
  { path: "/admin/orders", title: "주문·배송 관리", value: `${stats.value.orders}건`, text: `배송 준비 ${stats.value.preparing}건 · 취소/반품 신청을 확인합니다.` },
  { path: "/admin/products", title: "상품 관리", value: `${stats.value.products}개`, text: `품절 ${stats.value.soldOut}개 · 판매와 재고를 관리합니다.` },
  { path: "/admin/members", title: "회원 관리", value: `${stats.value.members}명`, text: "활성·차단·비활성 회원을 조회합니다." },
  { path: "/admin/notices", title: "공지사항 관리", value: `${stats.value.notices}건`, text: "공지 등록·수정·삭제와 게시 현황을 관리합니다." },
  { path: "/admin/qna", title: "QnA 관리", value: `${stats.value.qnas}건`, text: "전체 문의와 답변 대기 문의를 확인합니다." },
]);
function timeout(task, ms = 5000) { return Promise.race([task, new Promise((_, reject) => setTimeout(() => reject(new Error("API 응답 시간 초과")), ms))]); }
function rowsOf(result) { return result.status === "fulfilled" && Array.isArray(result.value) ? result.value : null; }
async function loadSummary() {
  loading.value = true;
  const results = await Promise.allSettled([timeout(adminManagementService.getOrders()), timeout(adminManagementService.getProducts()), timeout(adminManagementService.getMembers()), timeout(adminManagementService.getNotices()), timeout(adminManagementService.getManagedQnas())]);
  const [orders, products, members, notices, qnas] = results.map(rowsOf);
  apiIssue.value = results.some((result) => result.status === "rejected");
  stats.value = { orders: orders?.length ?? 0, preparing: orders?.filter((row) => ["PREPARING", "READY"].includes(row.status || row.orderStatus)).length ?? 0, products: products?.length ?? 0, soldOut: products?.filter((row) => row.soldOut === true || Number(row.stock) === 0).length ?? 0, members: members?.length ?? 0, notices: notices?.length ?? 0, qnas: qnas?.length ?? 0 };
  refreshedAt.value = new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }); loading.value = false;
}
function logout() { setAccessToken(""); router.push("/login"); }
onMounted(loadSummary);
</script>
<template>
  <div class="admin-overview"><aside class="admin-sidebar"><b>PAWCAST</b><span>관리자 페이지</span><nav><RouterLink class="active" to="/admin/dashboard">대시보드</RouterLink><RouterLink to="/admin/sales">매출 분석</RouterLink><RouterLink to="/admin/orders">주문·배송 관리</RouterLink><RouterLink to="/admin/products">상품 관리</RouterLink><RouterLink to="/admin/members">회원 관리</RouterLink><RouterLink to="/admin/notices">공지사항 관리</RouterLink><RouterLink to="/admin/qna">QnA 관리</RouterLink></nav><button @click="logout">로그아웃</button></aside><main><header><small>ADMIN OVERVIEW</small><h1>관리자 대시보드</h1><p>각 운영 기능의 현재 현황과 바로가기를 제공합니다.</p><div class="status"><span v-if="loading">실제 API 데이터를 확인하는 중입니다.</span><span v-else-if="apiIssue" class="issue">일부 API가 응답하지 않습니다. 해당 화면에서 오류 원인과 임시 화면을 안내합니다.</span><span v-else>실제 API 기준 · {{ refreshedAt }} 갱신</span><button :disabled="loading" @click="loadSummary">새로고침</button></div></header><section><RouterLink v-for="item in menu" :key="item.path" :to="item.path"><small>{{ item.title }}</small><strong>{{ item.value }}</strong><p>{{ item.text }}</p><span>관리 화면 열기 →</span></RouterLink></section></main></div>
</template>
<style scoped>
.admin-overview{display:grid;grid-template-columns:240px minmax(0,1fr);min-height:100vh;background:#f7f5f2;color:#3d281b}.admin-sidebar{display:flex;flex-direction:column;gap:8px;background:#fffcf7;border-right:1px solid #eadfd3;padding:30px 18px}.admin-sidebar>b{font-size:26px;color:#875738}.admin-sidebar>span{color:#987e6b;font-size:12px}.admin-sidebar nav{display:grid;gap:6px;margin-top:22px}.admin-sidebar a,.admin-sidebar button{border:0;border-radius:10px;background:transparent;padding:14px;color:#4b3425;font:inherit;font-weight:800;text-align:left;text-decoration:none}.admin-sidebar a:hover,.admin-sidebar .active{background:#a66b3f;color:#fff}.admin-sidebar>button{margin-top:auto;border:1px solid #d8bca2;background:#fff}.admin-overview main{min-width:0;padding:48px clamp(24px,6vw,88px)}header small{color:#a66838;font-weight:900;letter-spacing:.12em}h1{margin:8px 0}header p{color:#917866}.status{display:flex;align-items:center;gap:14px;margin-top:18px;color:#806653;font-size:13px}.status button{border:1px solid #d9b898;border-radius:8px;background:#fff;padding:7px 10px;color:#7c4e30;font-weight:800}.issue{color:#a14d32}main section{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16px;margin-top:30px;max-width:1120px}main section a{border:1px solid #e8d8ca;border-radius:16px;background:#fff;padding:22px;color:inherit;text-decoration:none}main section a:hover{border-color:#a66b3f;transform:translateY(-2px)}main section small{display:block;color:#8a5b3d;font-weight:800}main section strong{display:block;margin:9px 0;font-size:25px}main section p{min-height:40px;color:#917866}main section span{color:#a66838;font-weight:800}@media(max-width:640px){.admin-overview{grid-template-columns:1fr}.admin-sidebar nav{grid-template-columns:repeat(2,1fr)}main section{grid-template-columns:1fr}}
</style>

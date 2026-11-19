<!-- 관리자 업무별 화면: API 연동 전에도 동일한 조회·처리 동선을 검수할 수 있도록 구성합니다. -->
<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { products } from "@/data/products";
import { notices as initialNotices } from "@/data/notices";
import { useAuthSession } from "@/stores/auth";
import { adminManagementService } from "@/services/adminManagementService";

const route = useRoute();
const router = useRouter();
const { setAccessToken } = useAuthSession();
const section = computed(() => route.params.section || "orders");
const labels = { orders: "주문·배송 관리", products: "상품 관리", members: "회원 관리", qna: "QnA 관리", notices: "공지사항 관리" };
const query = ref("");
const status = ref("ALL");
const dateRange = ref("today");
const memberMode = ref("card");
const replyTarget = ref(null);
const replyText = ref("");
const noticeForm = ref({ type: "공지", title: "", content: "" });
const noticeRows = ref(initialNotices.map((item) => ({ ...item })));
const apiNotice = ref("");
const managedProducts = ref([]);
const orders = ref([
  { id: "20261119-001", buyer: "test01", date: "2026.11.19 10:20", product: "더리얼 그레인프리 오븐베이크드", amount: 28000, status: "배송 준비", claim: "-" },
  { id: "20261118-008", buyer: "sumi", date: "2026.11.18 15:42", product: "포켄스 관절·뼈 영양제", amount: 35600, status: "배송중", claim: "-" },
  { id: "20261117-003", buyer: "pawuser", date: "2026.11.17 09:11", product: "ANF 오리 단일단백질", amount: 48400, status: "취소 요청", claim: "취소" },
]);
const members = ref([
  { id: "test01", name: "이수미", email: "test01@pawcast.kr", joined: "2026.10.02", status: "활성화", orders: 3 },
  { id: "pawuser", name: "김포캐", email: "pawuser@pawcast.kr", joined: "2026.10.19", status: "비활성화", orders: 1 },
  { id: "reviewer", name: "박리뷰", email: "reviewer@pawcast.kr", joined: "2026.11.03", status: "차단", orders: 0 },
]);
const qnas = ref([
  { id: 1, title: "사료 급여량이 궁금해요", author: "kim**", date: "2026.11.19", status: "답변 대기", answer: "" },
  { id: 2, title: "배송 일정 문의", author: "lee**", date: "2026.11.18", status: "답변 완료", answer: "주문 후 영업일 기준 1~3일 내 출고됩니다." },
]);

const filteredOrders = computed(() => orders.value.filter((item) =>
  (status.value === "ALL" || item.status === status.value) && JSON.stringify(item).includes(query.value)));
const filteredMembers = computed(() => members.value.filter((item) =>
  (status.value === "ALL" || item.status === status.value) && JSON.stringify(item).includes(query.value)));
const filteredQnas = computed(() => qnas.value.filter((item) =>
  (status.value === "ALL" || item.status === status.value) && JSON.stringify(item).includes(query.value)));
const filteredNotices = computed(() => noticeRows.value.filter((item) => JSON.stringify(item).includes(query.value)));
const rankedProducts = computed(() => [...(managedProducts.value.length ? managedProducts.value : products)].sort((a, b) => Number(b.salesCount || b.popularity || 0) - Number(a.salesCount || a.popularity || 0)));

function formatDate(value) { return value ? new Date(value).toLocaleDateString("ko-KR") : "-"; }
async function loadBackendData() {
  const results = await Promise.allSettled([
    adminManagementService.getOrders(), adminManagementService.getMembers(), adminManagementService.getManagedQnas(), adminManagementService.getNotices(), adminManagementService.getProducts(),
  ]);
  const [orderResult, memberResult, qnaResult, noticeResult, productResult] = results;
  if (orderResult.status === "fulfilled") orders.value = orderResult.value.map((item) => ({ id: item.id ?? item.orderId ?? item.orderNumber, backendId: item.id ?? item.orderId, buyer: item.member?.name || item.member?.userId || "-", date: formatDate(item.created_at || item.createdAt), product: item.productName || "주문 상품", amount: Number(item.final_amount ?? item.finalAmount ?? 0), status: item.status, claim: "-" }));
  if (memberResult.status === "fulfilled") members.value = memberResult.value.map((item) => ({ id: item.id, name: item.name || "-", userId: item.userId || "-", email: item.email || "-", joined: formatDate(item.createdAt), status: item.status, orders: 0 }));
  if (qnaResult.status === "fulfilled") qnas.value = qnaResult.value.map((item, index) => ({ id: item.id ?? item.qnaId ?? null, title: item.title, author: item.member?.name || item.member?.userId || "-", date: formatDate(item.createdAt), status: item.answer ? "답변 완료" : "답변 대기", answer: item.answer?.content || "" , fallbackKey: index }));
  if (noticeResult.status === "fulfilled") noticeRows.value = noticeResult.value.map((item, index) => ({ id: item.id ?? item.noticeId ?? null, title: item.title, content: item.content, type: item.noticeType, date: formatDate(item.createdAt), views: item.views || 0, pinned: item.pinned ?? item.isPinned, fallbackKey: index }));
  if (productResult.status === "fulfilled") managedProducts.value = productResult.value.map((item) => ({ id: item.id, name: item.name, stock: item.soldOut ? 0 : null, salesCount: item.salesCount || 0, reviewCount: item.reviewCount || 0, rating: item.averageRating || 0, variantId: item.variantId }));
  const failed = results.filter((result) => result.status === "rejected").length;
  if (failed) apiNotice.value = `${failed}개 API는 백엔드 응답 구조 또는 서버 오류로 임시 데이터를 표시합니다.`;
}

function exportRows(filename, rows) {
  const keys = rows.length ? Object.keys(rows[0]) : [];
  const csv = `\uFEFF${[keys, ...rows.map((row) => keys.map((key) => String(row[key] ?? "").replaceAll(",", " ")))].map((row) => row.join(",")).join("\n")}`;
  const link = document.createElement("a");
  link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  link.download = `${filename}-${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(link.href);
}
async function updateOrder(order, next) { if (order.backendId) await adminManagementService.changeOrderStatus(order.backendId, next); else apiNotice.value = "주문 목록 응답에 orderId가 없어 상태 변경은 임시 처리입니다."; order.status = next; }
async function updateMember(member, next) { await adminManagementService.changeMemberStatus(member.id, next); member.status = next; }
async function answerQna(item) {
  if (!replyText.value.trim()) return;
  if (item.id) await adminManagementService.answerQna(item.id, replyText.value.trim());
  else apiNotice.value = "QnA 목록 응답에 qnaId가 없어 답변 등록은 임시 처리입니다.";
  item.answer = replyText.value.trim(); item.status = "답변 완료"; replyText.value = ""; replyTarget.value = null;
}
async function createNotice() {
  if (!noticeForm.value.title.trim() || !noticeForm.value.content.trim()) return;
  const payload = { noticeType: noticeForm.value.type === "이벤트" ? "EVENT" : "NOTICE", title: noticeForm.value.title, content: noticeForm.value.content, isPinned: false };
  try { await adminManagementService.createNotice(payload); } catch { apiNotice.value = "공지 등록 API 요청 형식을 백엔드에서 @RequestBody로 보완하기 전까지는 임시 저장으로 표시합니다."; }
  noticeRows.value.unshift({ id: null, ...noticeForm.value, date: new Date().toISOString().slice(0, 10), views: 0, pinned: false });
  noticeForm.value = { type: "공지", title: "", content: "" };
}
async function removeNotice(id) { if (id) await adminManagementService.deleteNotice(id); else apiNotice.value = "공지 목록 응답에 noticeId가 없어 임시 등록 항목만 화면에서 삭제했습니다."; noticeRows.value = noticeRows.value.filter((item) => item.id !== id); }
function logout() { setAccessToken(""); router.push("/login"); }
onMounted(loadBackendData);
</script>

<template>
  <div class="admin-operations">
    <aside class="admin-unified-sidebar">
      <header class="admin-unified-brand"><b>PAWCAST</b><span>관리자 페이지</span></header>
      <nav class="admin-unified-nav">
        <RouterLink to="/admin/dashboard"><span>최종 대시보드</span></RouterLink>
        <RouterLink to="/admin/sales"><span>매출 분석</span></RouterLink>
        <RouterLink to="/admin/orders" :class="{ active: section === 'orders' }"><span>주문·배송 관리</span></RouterLink>
        <RouterLink to="/admin/products" :class="{ active: section === 'products' }"><span>상품 관리</span></RouterLink>
        <RouterLink to="/admin/products/new"><span>상품 등록</span></RouterLink>
        <RouterLink to="/admin/members" :class="{ active: section === 'members' }"><span>회원 관리</span></RouterLink>
        <RouterLink to="/admin/notices" :class="{ active: section === 'notices' }"><span>공지사항 관리</span></RouterLink>
        <RouterLink to="/admin/qna" :class="{ active: section === 'qna' }"><span>QnA 관리</span></RouterLink>
      </nav>
      <button type="button" class="admin-unified-logout" @click="logout">로그아웃</button>
    </aside>

    <main class="operations-main">
      <header class="operations-heading"><div><small>ADMIN MANAGEMENT</small><h1>{{ labels[section] }}</h1></div><p>필요한 정보만 빠르게 확인하고 처리하세요.</p></header>
      <p v-if="apiNotice" class="api-notice">{{ apiNotice }}</p>
      <section v-if="section !== 'products' && section !== 'notices'" class="operations-filter">
        <label>조회 기간<select v-model="dateRange"><option value="today">오늘</option><option value="week">최근 7일</option><option value="month">최근 30일</option><option value="custom">직접 설정</option></select></label>
        <label>상태<select v-model="status"><option value="ALL">전체</option><option v-if="section === 'orders'">배송 준비</option><option v-if="section === 'orders'">배송중</option><option v-if="section === 'orders'">취소 요청</option><option v-if="section === 'members'">활성화</option><option v-if="section === 'members'">비활성화</option><option v-if="section === 'members'">차단</option><option v-if="section === 'qna'">답변 대기</option><option v-if="section === 'qna'">답변 완료</option></select></label>
        <label class="wide">검색<input v-model.trim="query" placeholder="주문번호, 상품명, 회원명, 제목 검색" /></label>
      </section>

      <template v-if="section === 'orders'">
        <section class="summary-cards"><article><small>오늘 주문</small><b>12건</b></article><article><small>배송 준비</small><b>7건</b></article><article><small>취소·반품 요청</small><b>2건</b></article></section>
        <section class="operations-panel"><header><h2>주문 현황</h2><button @click="exportRows('orders', filteredOrders)">엑셀 다운로드</button></header><div class="data-table"><table><thead><tr><th>주문번호</th><th>주문자</th><th>상품</th><th>금액</th><th>상태</th><th>처리</th></tr></thead><tbody><tr v-for="item in filteredOrders" :key="item.id"><td>{{ item.id }}</td><td>{{ item.buyer }}</td><td>{{ item.product }}</td><td>{{ item.amount.toLocaleString() }}원</td><td><b>{{ item.status }}</b></td><td><button v-if="item.status === '배송 준비'" @click="updateOrder(item, '배송중')">배송 처리</button><button v-else-if="item.status === '취소 요청'" @click="updateOrder(item, '취소 확정')">취소 확정</button><span v-else>처리 완료</span></td></tr></tbody></table></div></section>
      </template>

      <template v-else-if="section === 'products'">
        <section class="summary-cards"><article><small>판매 중 상품</small><b>{{ products.length }}개</b></article><article><small>재고 부족 상품</small><b>{{ products.filter((item) => item.stock > 0 && item.stock < 10).length }}개</b></article><article><small>품절 상품</small><b>{{ products.filter((item) => !item.stock).length }}개</b></article></section>
        <section class="operations-panel product-ranking"><header><h2>상품 판매 현황</h2><button @click="exportRows('products', rankedProducts)">엑셀 다운로드</button></header><div><article><h3>베스트 3</h3><ol><li v-for="item in rankedProducts.slice(0, 3)" :key="item.id"><span>{{ item.name }}</span><b>{{ Number(item.salesCount || item.popularity || 0).toLocaleString() }}</b></li></ol></article><article><h3>워스트 3</h3><ol><li v-for="item in rankedProducts.slice(-3).reverse()" :key="item.id"><span>{{ item.name }}</span><b>{{ Number(item.salesCount || item.popularity || 0).toLocaleString() }}</b></li></ol></article></div></section>
        <section class="operations-panel"><header><h2>상품별 재고·리뷰·판매 추이</h2></header><div class="product-health"><article v-for="item in rankedProducts.slice(0, 6)" :key="item.id"><b>{{ item.name }}</b><small>재고 {{ item.stock }}개 · 리뷰 긍정 78% / 부정 22%</small><div><i :style="{ width: `${Math.min(100, Number(item.popularity || 0) / 2)}%` }" /></div></article></div><p class="api-note">리뷰 긍정·부정 비율은 AI 감정 분석 API 데이터가 누적되면 실제 수치로 대체됩니다.</p></section>
      </template>

      <template v-else-if="section === 'members'">
        <section class="view-switch"><button :class="{ active: memberMode === 'card' }" @click="memberMode = 'card'">카드형</button><button :class="{ active: memberMode === 'list' }" @click="memberMode = 'list'">리스트형</button><button @click="exportRows('members', filteredMembers)">엑셀 다운로드</button></section>
        <section :class="['member-list', memberMode]"><article v-for="item in filteredMembers" :key="item.id"><div><b>{{ item.name }}</b><span>{{ item.id }} · {{ item.email }}</span><small>가입일 {{ item.joined }} · 주문 {{ item.orders }}건</small></div><label><select :value="item.status" @change="updateMember(item, $event.target.value)"><option>활성화</option><option>비활성화</option><option>차단</option></select></label></article></section>
      </template>

      <template v-else-if="section === 'qna'">
        <section class="operations-panel"><header><h2>전체 문의</h2><span>답변 대기 {{ qnas.filter((item) => item.status === '답변 대기').length }}건</span></header><div class="qna-admin-list"><article v-for="item in filteredQnas" :key="item.id"><div><small>{{ item.status }}</small><b>{{ item.title }}</b><span>{{ item.author }} · {{ item.date }}</span><p v-if="item.answer">{{ item.answer }}</p></div><button v-if="item.status === '답변 대기'" @click="replyTarget = item">답변하기</button></article></div></section>
        <section v-if="replyTarget" class="operations-panel reply-box"><h2>{{ replyTarget.title }}</h2><textarea v-model="replyText" placeholder="고객에게 전달할 답변을 입력하세요." /><footer><button @click="replyTarget = null">취소</button><button class="primary" @click="answerQna(replyTarget)">답변 등록</button></footer></section>
      </template>

      <template v-else-if="section === 'notices'">
        <section class="operations-panel notice-form"><header><h2>공지·이벤트 등록</h2><small>이벤트의 진행 상태는 표시하지 않습니다.</small></header><div><select v-model="noticeForm.type"><option>공지</option><option>이벤트</option></select><input v-model.trim="noticeForm.title" placeholder="제목" /><textarea v-model.trim="noticeForm.content" placeholder="내용" /></div><button class="primary" @click="createNotice">등록</button></section>
        <section class="operations-panel"><header><h2>현재 등록 목록</h2><input v-model.trim="query" placeholder="제목 검색" /></header><div class="notice-admin-list"><article v-for="item in filteredNotices" :key="item.id"><span>{{ item.type }}</span><b>{{ item.title }}</b><small>{{ item.date }} · 조회 {{ item.views || 0 }}</small><button @click="removeNotice(item.id)">삭제</button></article></div></section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.admin-operations{display:grid;grid-template-columns:220px minmax(0,1fr);min-height:100vh;background:#f7f5f2;color:#3d281b}.operations-sidebar{position:sticky;top:0;display:flex;flex-direction:column;height:100vh;padding:28px 16px;border-right:1px solid #eadfd3;background:#fffcf7}.operations-sidebar header{display:grid;gap:4px;padding:0 10px 24px;border-bottom:1px solid #eadfd3}.operations-sidebar header b{color:#875738;font-size:24px}.operations-sidebar header span{color:#987e6b;font-size:12px}.operations-sidebar nav{display:grid;gap:7px;margin-top:24px}.operations-sidebar a,.operations-sidebar>button{border:0;border-radius:9px;background:transparent;padding:13px;color:#4b3425;font-weight:800;text-align:left;text-decoration:none}.operations-sidebar a:hover,.operations-sidebar a.active{background:#8b5e3c;color:#fff}.operations-sidebar>button{margin-top:auto;border:1px solid #d7b892;text-align:center}.operations-main{width:min(1280px,100%);box-sizing:border-box;margin:auto;padding:38px clamp(20px,4vw,58px)}.operations-heading{display:flex;justify-content:space-between;align-items:end;margin-bottom:24px}.operations-heading small{color:#a66838;font-weight:900;letter-spacing:.12em}.operations-heading h1{margin:6px 0 0;font-size:34px}.operations-heading p{margin:0;color:#987e6b}.operations-filter,.operations-panel,.summary-cards article,.view-switch{border:1px solid #ead7c3;border-radius:15px;background:#fff;padding:20px}.operations-filter{display:grid;grid-template-columns:180px 180px minmax(200px,1fr);gap:14px;margin-bottom:18px}.operations-filter label{display:grid;gap:7px;color:#725945;font-size:13px;font-weight:800}.operations-filter :is(input,select),.notice-form :is(input,select,textarea),.reply-box textarea{box-sizing:border-box;width:100%;border:1px solid #dfcbb7;border-radius:8px;padding:11px;background:#fffdf9;font:inherit}.summary-cards{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-bottom:18px}.summary-cards article{display:grid;gap:6px}.summary-cards small{color:#907662}.summary-cards b{font-size:27px}.operations-panel{margin-bottom:18px}.operations-panel>header{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:16px}.operations-panel h2{margin:0;font-size:20px}.operations-panel button,.view-switch button{border:1px solid #c99972;border-radius:8px;background:#fff;padding:9px 13px;color:#684630;font-weight:800}.operations-panel button.primary,.reply-box .primary{background:#8b5e3c;color:#fff}.data-table{overflow:auto}.data-table table{width:100%;border-collapse:collapse;min-width:720px}.data-table th,.data-table td{border-bottom:1px solid #f0e5db;padding:13px 10px;text-align:left;font-size:13px}.data-table th{color:#83644d}.product-ranking>div{display:grid;grid-template-columns:1fr 1fr;gap:16px}.product-ranking article{border:1px solid #eee0d2;border-radius:11px;padding:16px}.product-ranking h3{margin:0 0 12px}.product-ranking ol{display:grid;gap:11px;margin:0;padding-left:24px}.product-ranking li{display:flex;justify-content:space-between;gap:12px}.product-ranking li span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.product-health{display:grid;grid-template-columns:repeat(2,1fr);gap:13px}.product-health article{display:grid;gap:7px;border:1px solid #eee0d2;border-radius:10px;padding:13px}.product-health small,.api-note{color:#967a65;font-size:12px}.product-health div{height:8px;overflow:hidden;border-radius:99px;background:#f1e5d9}.product-health i{display:block;height:100%;border-radius:inherit;background:#b47848}.api-note{margin:14px 0 0}.view-switch{display:flex;justify-content:flex-end;gap:8px;margin-bottom:18px}.view-switch button:first-child{margin-right:auto}.view-switch button.active{background:#8b5e3c;color:#fff}.member-list{display:grid;gap:11px}.member-list.card{grid-template-columns:repeat(3,1fr)}.member-list article{display:flex;justify-content:space-between;gap:15px;border:1px solid #ead7c3;border-radius:13px;background:#fff;padding:17px}.member-list article>div{display:grid;gap:6px}.member-list span,.member-list small{color:#977c68;font-size:13px}.member-list select{border:1px solid #dbb994;border-radius:7px;padding:8px;background:#fff;font:inherit}.qna-admin-list{display:grid;gap:10px}.qna-admin-list article,.notice-admin-list article{display:flex;justify-content:space-between;align-items:center;gap:15px;border-top:1px solid #f0e4d8;padding:13px 2px}.qna-admin-list article>div{display:grid;gap:5px}.qna-admin-list small{color:#ae713e;font-weight:800}.qna-admin-list span,.qna-admin-list p{margin:0;color:#927661;font-size:13px}.reply-box{display:grid;gap:13px}.reply-box textarea{min-height:140px;resize:vertical}.reply-box footer{display:flex;justify-content:flex-end;gap:8px}.notice-form>div{display:grid;grid-template-columns:120px 1fr;gap:10px;margin-bottom:10px}.notice-form textarea{grid-column:1/-1;min-height:140px;resize:vertical}.notice-admin-list article span{border-radius:20px;background:#f8ebd8;padding:5px 9px;color:#9a6139;font-size:12px;font-weight:800}.notice-admin-list article b{flex:1}.notice-admin-list article small{color:#957966}@media(max-width:900px){.admin-operations{grid-template-columns:1fr}.operations-sidebar{position:static;height:auto}.operations-sidebar nav{grid-template-columns:repeat(2,1fr)}.operations-sidebar>button{margin-top:18px}.operations-filter,.product-health,.member-list.card{grid-template-columns:1fr}.operations-heading{align-items:start;flex-direction:column}.summary-cards{grid-template-columns:1fr}.notice-form>div{grid-template-columns:1fr}}@media(max-width:560px){.operations-sidebar nav{grid-template-columns:1fr}.operations-main{padding:25px 14px}.product-ranking>div{grid-template-columns:1fr}.qna-admin-list article,.notice-admin-list article{align-items:start;flex-direction:column}.notice-admin-list article button{align-self:end}}
</style>

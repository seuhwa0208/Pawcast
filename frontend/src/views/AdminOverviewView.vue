<!-- 최종 대시보드는 이동 수단이 아닌 관리자 핵심 현황을 요약한 읽기 전용 화면입니다. -->
<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthSession } from "@/stores/auth";

const router = useRouter();
const { setAccessToken } = useAuthSession();

const today = new Date().toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric", weekday: "short" });
const weeklySales = [52, 68, 43, 81, 72, 94, 76];
const maximumSales = Math.max(...weeklySales);
const salesBars = computed(() => weeklySales.map((value, index) => ({ label: ["금", "토", "일", "월", "화", "수", "목"][index], height: `${Math.max(14, Math.round((value / maximumSales) * 100))}%`, value: `${value}만` })));
const categoryRows = [
  { label: "강아지", value: 48, color: "#a96e42" },
  { label: "고양이", value: 34, color: "#c98b58" },
  { label: "식기·급수", value: 18, color: "#e5bd88" },
];
function logout() { setAccessToken(""); router.push("/login"); }
</script>

<template>
  <div class="admin-operations">
    <aside class="admin-unified-sidebar">
      <header class="admin-unified-brand"><b>PAWCAST</b><span>관리자 페이지</span></header>
      <nav class="admin-unified-nav">
        <RouterLink class="active" to="/admin/dashboard"><span>최종 대시보드</span></RouterLink>
        <RouterLink to="/admin/sales"><span>매출 분석</span></RouterLink>
        <RouterLink to="/admin/orders"><span>주문·배송 관리</span></RouterLink>
        <RouterLink to="/admin/products"><span>상품 관리</span></RouterLink>
        <RouterLink to="/admin/products/new"><span>상품 등록</span></RouterLink>
        <RouterLink to="/admin/members"><span>회원 관리</span></RouterLink>
        <RouterLink to="/admin/notices"><span>공지사항 관리</span></RouterLink>
        <RouterLink to="/admin/qna"><span>QnA 관리</span></RouterLink>
      </nav>
      <button type="button" class="admin-unified-logout" @click="logout">로그아웃</button>
    </aside>
    <main class="overview-page">
    <header class="overview-heading"><div><small>ADMIN OVERVIEW</small><h1>최종 대시보드</h1><p>{{ today }} 기준 운영 핵심 현황입니다.</p></div><span>실시간 요약</span></header>
    <section class="overview-kpis" aria-label="핵심 운영 현황"><article><small>오늘 매출</small><strong>2,840,000원</strong><p>어제 대비 <b>▲ 8.4%</b></p></article><article><small>배송 준비</small><strong>7건</strong><p>오늘 처리할 주문</p></article><article><small>취소·반품 요청</small><strong>2건</strong><p>확정 처리 필요</p></article><article><small>답변 대기 QnA</small><strong>3건</strong><p>고객 답변 필요</p></article></section>
    <section class="overview-chart-grid">
      <article class="overview-panel weekly-chart"><header><div><h2>최근 7일 매출 추이</h2><p>일매출 기준</p></div><strong>이번 주 486만원</strong></header><div class="bar-chart"><div v-for="item in salesBars" :key="item.label" class="bar-item"><span>{{ item.value }}</span><i :style="{ height: item.height }" /><b>{{ item.label }}</b></div></div></article>
      <article class="overview-panel category-summary"><header><div><h2>카테고리 매출 비중</h2><p>이번 달 기준</p></div></header><div class="category-donut"><strong>100<small>%</small></strong></div><ul><li v-for="item in categoryRows" :key="item.label"><span :style="{ background: item.color }" />{{ item.label }}<b>{{ item.value }}%</b></li></ul></article>
    </section>
    <section class="overview-alerts overview-panel"><header><div><h2>확인 필요한 운영 항목</h2><p>우선 처리 순서로 정리했습니다.</p></div></header><div><article><span class="warning">재고</span><b>재고 부족 상품 4개</b><p>안전 재고 미만 상품을 확인하세요.</p></article><article><span class="notice">리뷰</span><b>부정 리뷰 2건</b><p>신규 등록 리뷰의 내용을 확인하세요.</p></article><article><span class="info">회원</span><b>신규 가입 5명</b><p>오늘 가입한 회원 수입니다.</p></article></div></section>
    </main>
  </div>
</template>

<style scoped>
.overview-page{min-height:100vh;box-sizing:border-box;background:#f7f5f2;padding:44px clamp(24px,5vw,80px);color:#3d281b}.overview-heading,.overview-kpis,.overview-chart-grid,.overview-alerts{width:min(1180px,100%);margin-inline:auto}.overview-heading{display:flex;align-items:end;justify-content:space-between;margin-bottom:23px}.overview-heading small{color:#a66838;font-size:12px;font-weight:900;letter-spacing:.14em}.overview-heading h1{margin:7px 0;font-size:36px}.overview-heading p{margin:0;color:#917866}.overview-heading>span{border:1px solid #e1c8b1;border-radius:99px;background:#fffaf5;padding:8px 12px;color:#8b5e3c;font-size:12px;font-weight:900}.overview-kpis{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:13px;margin-bottom:15px}.overview-kpis article,.overview-panel{border:1px solid #e8d8ca;border-radius:15px;background:#fff;padding:20px}.overview-kpis small{color:#907360;font-weight:800}.overview-kpis strong{display:block;margin:7px 0;font-size:22px}.overview-kpis p,.overview-panel header p{margin:0;color:#9a806d;font-size:12px}.overview-kpis b{color:#2f8c58}.overview-chart-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(280px,.8fr);gap:15px;margin-bottom:15px}.overview-panel header{display:flex;align-items:start;justify-content:space-between;gap:14px}.overview-panel h2{margin:0 0 5px;font-size:19px}.overview-panel header>strong{color:#9b643d;font-size:14px}.bar-chart{display:flex;align-items:end;justify-content:space-between;gap:12px;height:220px;margin-top:20px;border-bottom:1px solid #ebdfd5}.bar-item{position:relative;display:grid;flex:1;height:100%;align-items:end;justify-items:center;gap:8px}.bar-item i{display:block;width:min(38px,80%);border-radius:8px 8px 0 0;background:linear-gradient(#c98b58,#9b653e)}.bar-item span{align-self:end;margin-bottom:8px;color:#9b643d;font-size:11px}.bar-item b{padding-bottom:7px;color:#806653;font-size:12px}.category-summary{display:grid;grid-template-columns:118px 1fr;align-items:center;gap:15px}.category-summary header{grid-column:1/-1}.category-donut{display:grid;place-items:center;width:112px;height:112px;border-radius:50%;background:conic-gradient(#a96e42 0 48%,#c98b58 48% 82%,#e5bd88 82% 100%)}.category-donut::before{grid-area:1/1;width:76px;height:76px;border-radius:50%;background:#fff;content:""}.category-donut strong{grid-area:1/1;z-index:1;color:#684633;font-size:20px}.category-donut small{font-size:11px}.category-summary ul{display:grid;gap:9px;margin:0;padding:0;list-style:none}.category-summary li{display:flex;align-items:center;gap:7px;color:#7d6250;font-size:13px}.category-summary li span{width:9px;height:9px;border-radius:50%}.category-summary li b{margin-left:auto;color:#593d2c}.overview-alerts>div{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:15px}.overview-alerts article{border:1px solid #f0e2d5;border-radius:10px;padding:14px}.overview-alerts article span{display:inline-block;margin-bottom:8px;border-radius:99px;padding:4px 8px;font-size:11px;font-weight:900}.overview-alerts b{display:block}.overview-alerts article p{margin:6px 0 0;color:#987d69;font-size:12px}.warning{background:#fff1d9;color:#a5681e}.notice{background:#fff0ed;color:#bd6546}.info{background:#f0f3ff;color:#596baf}@media(max-width:850px){.overview-kpis,.overview-alerts>div{grid-template-columns:repeat(2,1fr)}.overview-chart-grid{grid-template-columns:1fr}}@media(max-width:520px){.overview-page{padding:30px 15px}.overview-heading{align-items:start;gap:13px;flex-direction:column}.overview-kpis,.overview-alerts>div{grid-template-columns:1fr}.category-summary{grid-template-columns:112px 1fr}.bar-chart{gap:5px}}
</style>

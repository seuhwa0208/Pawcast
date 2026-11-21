<!-- 관리자 API 상태를 한눈에 보여주고, 실패 시에는 명시적으로 대체 상태를 표시합니다. -->
<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { adminManagementService } from "@/services/adminManagementService";
import { adminDashboardService } from "@/services/adminDashboardService";
import AdminSidebar from "@/components/AdminSidebar.vue";

// # ECharts는 관리자 영역에 들어왔을 때만 불러와 일반 쇼핑 화면의 초기 용량을 지킨다.
let echartsLoader;
function loadEcharts() {
  echartsLoader ||= Promise.all([
    import("echarts/charts"), import("echarts/components"),
    import("echarts/core"), import("echarts/renderers"),
  ]).then(([charts, components, core, renderers]) => {
    core.use([charts.BarChart, charts.LineChart, charts.PieChart, components.GridComponent, components.LegendComponent, components.TooltipComponent, renderers.CanvasRenderer]);
    return core.init;
  });
  return echartsLoader;
}

const loading = ref(true);
const apiIssue = ref(false);
const refreshedAt = ref("");
const stats = ref({
  orders: 0,
  preparing: 0,
  products: 0,
  soldOut: 0,
  members: 0,
  notices: 0,
  qnas: 0,
});
const salesDashboard = ref(null);
const salesChartElement = ref(null);
const categoryChartElement = ref(null);
let salesChart;
let categoryChart;
let chartResizeObserver;
const salesSummary = computed(() => salesDashboard.value?.summary || { totalSales: 0, totalOrders: 0, averageOrderAmount: 0, refundAmount: 0 });
const formatWon = (value) => `${Number(value || 0).toLocaleString()}원`;
const menu = computed(() => [
  {
    path: "/admin/sales",
    title: "매출 분석",
    value: "일·월·분기·연도",
    text: "기간과 카테고리 기준 매출을 조회합니다.",
  },
  {
    path: "/admin/orders",
    title: "주문·배송 관리",
    value: `${stats.value.orders}건`,
    text: `배송 준비 ${stats.value.preparing}건 · 취소/반품 신청을 확인합니다.`,
  },
  {
    path: "/admin/products",
    title: "상품 관리",
    value: `${stats.value.products}개`,
    text: `품절 ${stats.value.soldOut}개 · 판매와 재고를 관리합니다.`,
  },
  {
    path: "/admin/members",
    title: "회원 관리",
    value: `${stats.value.members}명`,
    text: "활성·차단·비활성 회원을 조회합니다.",
  },
  {
    path: "/admin/notices",
    title: "공지사항 관리",
    value: `${stats.value.notices}건`,
    text: "공지 등록·수정·삭제와 게시 현황을 관리합니다.",
  },
  {
    path: "/admin/qna",
    title: "QnA 관리",
    value: `${stats.value.qnas}건`,
    text: "전체 문의와 답변 대기 문의를 확인합니다.",
  },
]);
function timeout(task, ms = 5000) {
  return Promise.race([
    task,
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error("API 응답 시간 초과")), ms),
    ),
  ]);
}
function rowsOf(result) {
  return result.status === "fulfilled" && Array.isArray(result.value)
    ? result.value
    : null;
}
async function loadSummary() {
  loading.value = true;
  const year = new Date().getFullYear();
  const results = await Promise.allSettled([
    timeout(adminDashboardService.getSalesDashboard({ unit: "month", year, from: `${year}-01-01`, to: `${year}-12-31` })),
    timeout(adminManagementService.getOrders()),
    timeout(adminManagementService.getProducts()),
    timeout(adminManagementService.getMembers()),
    timeout(adminManagementService.getNotices()),
    timeout(adminManagementService.getManagedQnas()),
  ]);
  const [dashboardResult, orders, products, members, notices, qnas] = results;
  const dashboard = dashboardResult.status === "fulfilled" ? dashboardResult.value : null;
  const [orderRows, productRows, memberRows, noticeRows, qnaRows] = [orders, products, members, notices, qnas].map(rowsOf);
  apiIssue.value = results.some((result) => result.status === "rejected");
  salesDashboard.value = dashboard;
  stats.value = {
    orders: orderRows?.length ?? 0,
    preparing:
      orderRows?.filter((row) =>
        ["PREPARING", "READY"].includes(row.status || row.orderStatus),
      ).length ?? 0,
    products: productRows?.length ?? 0,
    soldOut:
      productRows?.filter((row) => row.soldOut === true || Number(row.stock) === 0)
        .length ?? 0,
    members: memberRows?.length ?? 0,
    notices: noticeRows?.length ?? 0,
    qnas: qnaRows?.length ?? 0,
  };
  refreshedAt.value = new Date().toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  });
  loading.value = false;
}
async function renderCharts() {
  if (!salesChartElement.value || !categoryChartElement.value) return;
  const init = await loadEcharts();
  salesChart ||= init(salesChartElement.value);
  categoryChart ||= init(categoryChartElement.value);
  const trend = salesDashboard.value?.trend || { labels: [], sales: [], orders: [] };
  const categories = salesDashboard.value?.categories || [];
  salesChart.setOption({ color:["#b77949", "#5f3c2a"], tooltip:{trigger:"axis"}, legend:{top:0}, grid:{top:42,right:40,bottom:28,left:54}, xAxis:{type:"category",data:trend.labels}, yAxis:[{type:"value",name:"매출액"},{type:"value",name:"주문"}], series:[{name:"매출액",type:"bar",data:trend.sales,barMaxWidth:38,itemStyle:{borderRadius:[5,5,0,0]}},{name:"주문 건수",type:"line",yAxisIndex:1,data:trend.orders,smooth:true,symbolSize:7}] }, true);
  categoryChart.setOption({ color:["#9b653e", "#9eaf7c", "#efd7ad"], tooltip:{trigger:"item",formatter:"{b}<br/>{c}원 ({d}%)"}, legend:{bottom:0}, series:[{type:"pie",radius:["48%","72%"],center:["50%","44%"],label:{formatter:"{b}\n{d}%"},data:categories.map((row)=>({name:row.name,value:row.sales}))}] }, true);
}
watch(salesDashboard, async () => { await nextTick(); await renderCharts(); });
onMounted(async () => {
  await loadSummary();
  // watch 타이밍과 무관하게 최초 진입에도 차트를 한 번 그린다.
  await nextTick();
  await renderCharts();
  chartResizeObserver = new ResizeObserver(() => { salesChart?.resize(); categoryChart?.resize(); });
  if (salesChartElement.value) chartResizeObserver.observe(salesChartElement.value);
  if (categoryChartElement.value) chartResizeObserver.observe(categoryChartElement.value);
});
onBeforeUnmount(() => { chartResizeObserver?.disconnect(); salesChart?.dispose(); categoryChart?.dispose(); });
</script>
<template>
  <div class="admin-overview">
    <AdminSidebar />
    <main>
      <header>
        <small>ADMIN OVERVIEW</small>
        <h1>관리자 대시보드</h1>
        <p>운영에 필요한 핵심 현황을 실제 API 기준으로 표시합니다.</p>
        <div class="status">
          <span v-if="loading">실제 API 데이터를 확인하는 중입니다.</span
          ><span v-else-if="apiIssue" class="issue"
            >일부 API가 응답하지 않습니다. 해당 화면에서 오류 원인과 임시 화면을
            안내합니다.</span
          ><span v-else>실제 API 기준 · {{ refreshedAt }} 갱신</span
          ><button :disabled="loading" @click="loadSummary">새로고침</button>
        </div>
      </header>
      <section class="overview-metric-grid" aria-label="매출 핵심 지표">
        <RouterLink to="/admin/sales"><small>총 매출</small><strong>{{ formatWon(salesSummary.totalSales) }}</strong><p>환불 승인 금액 {{ formatWon(salesSummary.refundAmount) }}</p><span>매출 분석 →</span></RouterLink>
        <RouterLink to="/admin/sales"><small>주문 건수</small><strong>{{ salesSummary.totalOrders.toLocaleString() }}건</strong><p>평균 주문금액 {{ formatWon(salesSummary.averageOrderAmount) }}</p><span>매출 분석 →</span></RouterLink>
        <RouterLink to="/admin/orders"><small>배송 준비</small><strong>{{ stats.preparing }}건</strong><p>주문 상태와 환불 신청을 건별 처리합니다.</p><span>주문·배송 관리 →</span></RouterLink>
        <RouterLink to="/admin/products"><small>재고 확인</small><strong>{{ stats.soldOut }}개</strong><p>품절 상품과 판매·리뷰 현황을 확인합니다.</p><span>상품 관리 →</span></RouterLink>
      </section>
      <section class="overview-chart-grid" aria-label="매출 차트">
        <RouterLink to="/admin/sales" class="overview-chart-panel"><header><div><h2>월별 매출 추이</h2><p>막대: 매출액 · 선: 주문 건수</p></div><span>상세 분석 →</span></header><div ref="salesChartElement" class="overview-echart" /></RouterLink>
        <RouterLink to="/admin/sales" class="overview-chart-panel"><header><div><h2>카테고리별 매출 비중</h2><p>현재 연도 기준</p></div><span>상세 분석 →</span></header><div ref="categoryChartElement" class="overview-echart" /></RouterLink>
      </section>
      <section class="overview-menu-grid">
        <RouterLink v-for="item in menu" :key="item.path" :to="item.path"
          ><small>{{ item.title }}</small
          ><strong>{{ item.value }}</strong>
          <p>{{ item.text }}</p>
          <span>관리 화면 열기 →</span></RouterLink
        >
      </section>
    </main>
  </div>
</template>

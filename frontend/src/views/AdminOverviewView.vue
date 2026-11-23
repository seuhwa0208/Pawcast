<!-- 관리자 API 상태를 한눈에 보여주고, 실패 시에는 명시적으로 대체 상태를 표시합니다. -->
<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { adminManagementService } from "@/services/adminManagementService";
import { adminDashboardService } from "@/services/adminDashboardService";
import { aiAnalyticsService } from "@/services/aiAnalyticsService";

// # ECharts는 관리자 영역에 들어왔을 때만 불러와 일반 쇼핑 화면의 초기 용량을 지킨다.
let echartsLoader;

function loadEcharts() {
  echartsLoader ||= Promise.all([
    import("echarts/charts"),
    import("echarts/components"),
    import("echarts/core"),
    import("echarts/renderers"),
  ]).then(([charts, components, core, renderers]) => {
    core.use([
      charts.BarChart,
      charts.LineChart,
      charts.PieChart,
      components.GridComponent,
      components.LegendComponent,
      components.TooltipComponent,
      renderers.CanvasRenderer,
    ]);
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

// # 주문·배송 관리 / 상품 관리 / QnA 관리의 실제 목록을 통합 화면에 요약한다.
const orders = ref([]);
const refunds = ref([]);
const products = ref([]);
const productHighlights = ref({ best: [], worst: [] });
const managedQnas = ref([]);
const salesDashboard = ref(null);
const sentimentSummary = ref(null);
const inventoryForecast = ref(null);

const salesChartElement = ref(null);
const categoryChartElement = ref(null);

let salesChart;
let categoryChart;
let chartResizeObserver;

const salesSummary = computed(
  () =>
    salesDashboard.value?.summary || {
      totalSales: 0,
      totalOrders: 0,
      averageOrderAmount: 0,
      refundAmount: 0,
    },
);

const sentiment = computed(
  () =>
    sentimentSummary.value || {
      analyzedCount: 0,
      positiveRate: 0,
      negativeRate: 0,
      negativeProducts: [],
    },
);

const inventoryItems = computed(() => inventoryForecast.value?.items || []);
const pendingRefunds = computed(() => refunds.value.filter((row) => row.status === "PENDING"));
const waitingQnas = computed(() => managedQnas.value.filter((row) => row.status !== "ANSWERED"));

const orderStatuses = computed(() => {
  const options = [
    ["PAID", "결제 완료", "paid"],
    ["PREPARING", "상품 준비", "preparing"],
    ["SHIPPING", "배송 중", "shipping"],
    ["DELIVERED", "배송 완료", "delivered"],
    ["CANCELLED", "취소", "cancelled"],
  ];
  const total = Math.max(1, orders.value.length);

  return options.map(([key, label, tone]) => {
    const count = orders.value.filter((row) => (row.status || row.orderStatus) === key).length;
    return {
      key,
      label,
      tone,
      count,
      percent: Math.round((count / total) * 100),
    };
  });
});

const topProducts = computed(() => [
  ...productHighlights.value.best.map((row, index) => ({
    ...row,
    rank: `BEST ${index + 1}`,
    tone: "best",
  })),
  ...productHighlights.value.worst.map((row, index) => ({
    ...row,
    rank: `WORST ${index + 1}`,
    tone: "worst",
  })),
]);

const formatWon = (value) => `${Number(value || 0).toLocaleString()}원`;

const formatDateTime = (value) =>
  value
    ? new Intl.DateTimeFormat("ko-KR", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      }).format(new Date(value))
    : "-";

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
    timeout(
      adminDashboardService.getSalesDashboard({
        unit: "month",
        year,
        from: `${year}-01-01`,
        to: `${year}-12-31`,
      }),
    ),
    timeout(adminManagementService.getOrders()),
    timeout(adminManagementService.getRefunds()),
    timeout(adminManagementService.getProducts()),
    timeout(adminManagementService.getBestProducts()),
    timeout(adminManagementService.getWorstProducts()),
    timeout(adminManagementService.getMembers()),
    timeout(adminManagementService.getNotices()),
    timeout(adminManagementService.getManagedQnas()),
    timeout(aiAnalyticsService.getReviewSentimentSummary()),
    timeout(aiAnalyticsService.getInventoryForecast()),
  ]);

  const [
    dashboardResult,
    orderResult,
    refundResult,
    productResult,
    bestResult,
    worstResult,
    members,
    notices,
    qnas,
    sentimentResult,
    inventoryResult,
  ] = results;

  const dashboard = dashboardResult.status === "fulfilled" ? dashboardResult.value : null;
  const [
    orderRows,
    refundRows,
    productRows,
    bestRows,
    worstRows,
    memberRows,
    noticeRows,
    qnaRows,
  ] = [orderResult, refundResult, productResult, bestResult, worstResult, members, notices, qnas].map(
    rowsOf,
  );

  apiIssue.value = results.some((result) => result.status === "rejected");
  salesDashboard.value = dashboard;
  orders.value = orderRows || [];
  refunds.value = refundRows || [];
  products.value = productRows || [];
  productHighlights.value = { best: bestRows || [], worst: worstRows || [] };
  managedQnas.value = qnaRows || [];
  sentimentSummary.value = sentimentResult.status === "fulfilled" ? sentimentResult.value : null;
  inventoryForecast.value = inventoryResult.status === "fulfilled" ? inventoryResult.value : null;

  stats.value = {
    orders: orderRows?.length ?? 0,
    preparing:
      orderRows?.filter((row) =>
        ["PREPARING", "READY"].includes(row.status || row.orderStatus),
      ).length ?? 0,
    products: productRows?.length ?? 0,
    soldOut:
      productRows?.filter((row) => row.soldOut === true || Number(row.stock) === 0).length ?? 0,
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

  salesChart.setOption(
    {
      color: ["#b77949", "#5f3c2a"],
      tooltip: { trigger: "axis" },
      legend: { top: 0 },
      grid: { top: 42, right: 40, bottom: 28, left: 54 },
      xAxis: { type: "category", data: trend.labels },
      yAxis: [
        { type: "value", name: "매출액" },
        { type: "value", name: "주문" },
      ],
      series: [
        {
          name: "매출액",
          type: "bar",
          data: trend.sales,
          barMaxWidth: 38,
          itemStyle: { borderRadius: [5, 5, 0, 0] },
        },
        {
          name: "주문 건수",
          type: "line",
          yAxisIndex: 1,
          data: trend.orders,
          smooth: true,
          symbolSize: 7,
        },
      ],
    },
    true,
  );

  categoryChart.setOption(
    {
      color: ["#9b653e", "#9eaf7c", "#efd7ad"],
      tooltip: { trigger: "item", formatter: "{b}<br/>{c}원 ({d}%)" },
      legend: { bottom: 0 },
      series: [
        {
          type: "pie",
          radius: ["48%", "72%"],
          center: ["50%", "44%"],
          label: { formatter: "{b}\n{d}%" },
          data: categories.map((row) => ({ name: row.name, value: row.sales })),
        },
      ],
    },
    true,
  );
}

watch(salesDashboard, async () => {
  await nextTick();
  await renderCharts();
});

onMounted(async () => {
  await loadSummary();
  // watch 타이밍과 무관하게 최초 진입에도 차트를 한 번 그린다.
  await nextTick();
  await renderCharts();

  chartResizeObserver = new ResizeObserver(() => {
    salesChart?.resize();
    categoryChart?.resize();
  });

  if (salesChartElement.value) chartResizeObserver.observe(salesChartElement.value);
  if (categoryChartElement.value) chartResizeObserver.observe(categoryChartElement.value);
});

onBeforeUnmount(() => {
  chartResizeObserver?.disconnect();
  salesChart?.dispose();
  categoryChart?.dispose();
});
</script>

<template>
  <main class="admin-overview">
    <header>
      <small>ADMIN OVERVIEW</small>
      <h1>대시보드</h1>
      <p>운영 판단을 빠르게 하는 요약 화면 · 실제 API 기준</p>
      <div class="status">
        <span v-if="loading">실제 API 데이터를 확인하는 중입니다.</span>
        <span v-else-if="apiIssue" class="issue">
          일부 API가 응답하지 않습니다. 해당 화면에서 오류 원인과 임시 화면을 안내합니다.
        </span>
        <span v-else>실제 API 기준 · {{ refreshedAt }} 갱신</span>
        <button :disabled="loading" @click="loadSummary">새로고침</button>
      </div>
    </header>

    <!-- # 매출 분석 · 주문·배송 관리 · 상품 관리 · QnA 관리 핵심 지표 -->
    <section class="overview-metric-grid" aria-label="운영 핵심 지표">
      <RouterLink to="/admin/sales">
        <small>총 매출</small>
        <strong>{{ formatWon(salesSummary.totalSales) }}</strong>
        <p>환불 승인 금액 {{ formatWon(salesSummary.refundAmount) }}</p>
        <span>매출 분석 →</span>
      </RouterLink>

      <RouterLink to="/admin/sales">
        <small>주문 건수</small>
        <strong>{{ salesSummary.totalOrders.toLocaleString() }}건</strong>
        <p>평균 주문금액 {{ formatWon(salesSummary.averageOrderAmount) }}</p>
        <span>매출 분석 →</span>
      </RouterLink>

      <RouterLink to="/admin/orders">
        <small>배송 준비</small>
        <strong>{{ stats.preparing }}건</strong>
        <p>주문 상태와 환불 신청을 건별 처리합니다.</p>
        <span>주문·배송 관리 →</span>
      </RouterLink>

      <RouterLink to="/admin/products">
        <small>재고 확인</small>
        <strong>{{ stats.soldOut }}개</strong>
        <p>품절 상품과 판매·리뷰 현황을 확인합니다.</p>
        <span>상품 관리 →</span>
      </RouterLink>

      <RouterLink to="/admin/orders" class="attention">
        <small>환불 승인 대기</small>
        <strong>{{ pendingRefunds.length }}건</strong>
        <p>사유·금액 확인 후 승인 처리</p>
        <span>주문·배송 관리 →</span>
      </RouterLink>

      <RouterLink to="/admin/qna" class="attention">
        <small>답변 대기 QnA</small>
        <strong>{{ waitingQnas.length }}건</strong>
        <p>고객 문의 우선 답변</p>
        <span>QnA 관리 →</span>
      </RouterLink>
    </section>

    <section class="overview-chart-grid" aria-label="매출 차트">
      <RouterLink to="/admin/sales" class="overview-chart-panel">
        <header>
          <div>
            <h2>월별 매출 추이</h2>
            <p>막대: 매출액 · 선: 주문 건수</p>
          </div>
          <span>상세 분석 →</span>
        </header>
        <div ref="salesChartElement" class="overview-echart" />
      </RouterLink>

      <RouterLink to="/admin/sales" class="overview-chart-panel">
        <header>
          <div>
            <h2>카테고리별 매출 비중</h2>
            <p>현재 연도 기준</p>
          </div>
          <span>상세 분석 →</span>
        </header>
        <div ref="categoryChartElement" class="overview-echart" />
      </RouterLink>
    </section>

    <!-- # 주문·배송 관리: 실제 주문 상태 · 환불 승인 대기 / 상품 관리: AI 입고 우선순위 -->
    <section class="overview-operations-grid" aria-label="주문·환불·재고 운영 현황">
      <RouterLink to="/admin/orders" class="overview-operation-panel">
        <header>
          <h2>주문 상태 현황</h2>
          <span>주문 관리 →</span>
        </header>
        <ul class="status-summary-list">
          <li v-for="item in orderStatuses" :key="item.key">
            <div>
              <b :class="`status-chip ${item.tone}`">{{ item.label }}</b>
              <strong>{{ item.count }}건</strong>
            </div>
            <i><em :style="{ width: `${item.percent}%` }" /></i>
          </li>
        </ul>
      </RouterLink>

      <RouterLink to="/admin/orders" class="overview-operation-panel">
        <header>
          <div>
            <h2>환불 신청 · 승인 대기</h2>
            <p>사유와 금액을 확인해 승인합니다.</p>
          </div>
          <span>주문 관리 →</span>
        </header>
        <table v-if="pendingRefunds.length" class="compact-table">
          <thead>
            <tr>
              <th>사유</th>
              <th>금액</th>
              <th>신청</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pendingRefunds.slice(0, 5)" :key="item.refundId">
              <td>{{ item.reason }}</td>
              <td>{{ formatWon(item.refundAmount) }}</td>
              <td>{{ formatDateTime(item.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="overview-empty">승인 대기 환불이 없습니다.</p>
      </RouterLink>

      <RouterLink to="/admin/products" class="overview-operation-panel">
        <header>
          <div>
            <h2>입고 우선순위</h2>
            <p>최근 판매량 · 현재 옵션 재고 기준</p>
          </div>
          <span>상품 관리 →</span>
        </header>
        <ul v-if="inventoryItems.length" class="inventory-summary-list">
          <li v-for="item in inventoryItems.slice(0, 5)" :key="item.variantId">
            <div>
              <b>{{ item.productName }}</b>
              <small>{{ item.optionLabel }} · 재고 {{ item.stock }}개</small>
            </div>
            <span>
              {{ item.daysUntilStockout }}일 후<br>
              권장 {{ item.recommendedInboundQuantity }}개
            </span>
          </li>
        </ul>
        <p v-else class="overview-empty">예측할 판매 이력이 없습니다.</p>
      </RouterLink>
    </section>

    <section class="overview-ai-grid" aria-label="AI 운영 분석">
      <!-- # 상품 관리: 실제 판매량 기준 베스트/워스트 상품 -->
      <RouterLink to="/admin/products" class="overview-ai-panel product-performance-panel">
        <header>
          <div>
            <h2>상품 성과</h2>
            <p>판매량 기준 베스트 3 / 워스트 3</p>
          </div>
          <span>상품 관리 →</span>
        </header>
        <table v-if="topProducts.length" class="compact-table product-performance-table">
          <thead>
            <tr>
              <th>구분</th>
              <th>상품</th>
              <th>판매량</th>
              <th>평점</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in topProducts" :key="`${product.rank}-${product.id}`">
              <td><b :class="product.tone">{{ product.rank }}</b></td>
              <td>{{ product.name }}</td>
              <td>{{ Number(product.salesCount || 0).toLocaleString() }}건</td>
              <td>{{ product.averageRating || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="overview-empty">상품 성과 데이터가 없습니다.</p>
      </RouterLink>

      <!-- # 상품 관리: AI 감성 분석이 저장된 공개 리뷰만 집계 -->
      <RouterLink to="/admin/products" class="overview-ai-panel">
        <header>
          <div>
            <h2>AI 리뷰 감성 비율</h2>
            <p>AI 분석이 완료된 공개 리뷰 기준</p>
          </div>
          <span>상품 관리 →</span>
        </header>
        <template v-if="sentiment.analyzedCount">
          <div class="sentiment-rate" aria-label="리뷰 감성 비율">
            <span class="positive" :style="{ width: `${sentiment.positiveRate}%` }" />
            <span class="negative" :style="{ width: `${sentiment.negativeRate}%` }" />
          </div>
          <div class="sentiment-legend">
            <span>긍정 {{ sentiment.positiveRate }}%</span>
            <span>부정 {{ sentiment.negativeRate }}%</span>
            <small>분석 리뷰 {{ sentiment.analyzedCount }}건</small>
          </div>
          <ul class="ai-list">
            <li v-for="product in sentiment.negativeProducts.slice(0, 3)" :key="product.productId">
              <strong>{{ product.productName }}</strong>
              <span>부정 {{ product.negativeRate }}% · {{ product.negativeCount }}건</span>
            </li>
          </ul>
        </template>
        <p v-else class="empty-analysis">분석 완료된 공개 리뷰가 없어 감성 비율을 표시하지 않습니다.</p>
      </RouterLink>

      <!-- # 상품 관리: 최근 실제 판매량과 옵션 재고를 이용한 AI 입고 예측 -->
      <RouterLink to="/admin/products" class="overview-ai-panel inventory-ai-panel">
        <header>
          <div>
            <h2>AI 재고 소진 예측</h2>
            <p>최근 {{ inventoryForecast?.lookbackDays || 28 }}일 판매량 · 현재 옵션 재고</p>
          </div>
          <span>상품 관리 →</span>
        </header>
        <ul v-if="inventoryItems.length" class="ai-list inventory-list">
          <li v-for="item in inventoryItems.slice(0, 5)" :key="item.variantId">
            <div>
              <strong>{{ item.productName }}</strong>
              <small>{{ item.optionLabel }} · 현재 {{ item.stock }}개</small>
            </div>
            <span>
              {{ item.daysUntilStockout }}일 후 소진<br>
              권장 입고 {{ item.recommendedInboundQuantity }}개
            </span>
          </li>
        </ul>
        <p v-else class="empty-analysis">최근 판매 이력이 있는 판매중 옵션이 없어 재고 예측을 표시하지 않습니다.</p>
      </RouterLink>
    </section>

    <!-- # 매출 분석: AI 서버가 실제 주문 이력으로 계산한 다음 달 판매 예측 -->
    <RouterLink to="/admin/sales" class="overview-forecast-panel">
      <small>AI SALES FORECAST</small>
      <h2>다음 달 판매 예측</h2>
      <strong>{{ formatWon(salesDashboard?.forecast?.predictedSales) }}</strong>
      <p>{{ salesDashboard?.forecast?.insight || '판매 예측 데이터가 없습니다.' }}</p>
      <span>매출 분석에서 AI 재학습 →</span>
    </RouterLink>
  </main>
</template>
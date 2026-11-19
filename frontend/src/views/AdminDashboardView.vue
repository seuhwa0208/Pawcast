<!-- 판매자·관리자가 기간별 매출 실적과 AI 판매 예측을 검수하는 개발용 관리자 대시보드입니다. -->
<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { BarChart, LineChart, PieChart } from "echarts/charts";
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from "echarts/components";
import { init, use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { useRouter } from "vue-router";
import { adminDashboardService } from "@/services/adminDashboardService";
import { useAuthSession } from "@/stores/auth";

// 관리자 화면에서 사용하는 차트 종류만 등록해 고객 화면의 초기 번들 크기를 줄입니다.
use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  CanvasRenderer,
]);

const router = useRouter();
const { setAccessToken } = useAuthSession();
const today = new Date();
const currentYear = String(today.getFullYear());
const periodUnit = ref("day");
const selectedPeriod = ref(today.toISOString().slice(0, 10));
const selectedCategories = ref(["DOG", "CAT", "ALL"]);
const appliedAt = ref("초기 화면 데이터");
const currentDateTime = ref("");
const lastForecastDate = ref(
  localStorage.getItem("pawcast-ai-last-forecast-date") || "기록 없음",
);
const sidebarCollapsed = ref(false);
const dashboard = ref(null);
const loading = ref(false);
const forecastLoading = ref(false);
const lastTrainingOrderCount = ref(
  Number(localStorage.getItem("pawcast-ai-last-training-orders") || 0),
);
const lastTrainingDate = ref(
  localStorage.getItem("pawcast-ai-last-training-date") || "기록 없음",
);
const salesChartElement = ref(null);
const categoryChartElement = ref(null);
let salesChart;
let categoryChart;
let chartResizeObserver;
let clockTimer;

const summary = computed(
  () =>
    dashboard.value?.summary || {
      totalSales: 0,
      totalOrders: 0,
      averageOrderAmount: 0,
      refundAmount: 0,
    },
);
const activeData = computed(
  () => dashboard.value?.trend || { labels: [], sales: [], orders: [] },
);
const visibleCategories = computed(() => dashboard.value?.categories || []);
const forecast = computed(
  () =>
    dashboard.value?.forecast || {
      predictedSales: 0,
      growthRate: 0,
      insight: "예측 데이터를 불러오고 있습니다.",
      topProducts: [],
    },
);
const totalSales = computed(() => summary.value.totalSales);
const totalOrders = computed(() => summary.value.totalOrders);
const totalRefund = computed(() => summary.value.refundAmount);
const averageOrder = computed(() => summary.value.averageOrderAmount);
const ordersSinceTraining = computed(() =>
  Math.max(0, Number(totalOrders.value || 0) - lastTrainingOrderCount.value),
);
const retrainingRecommended = computed(() => ordersSinceTraining.value >= 100);

const formatWon = (value) => `${Math.round(value).toLocaleString()}원`;
const periodOptions = computed(() => {
  if (periodUnit.value === "day")
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(today);
      date.setDate(today.getDate() - index);
      const value = date.toISOString().slice(0, 10);
      return { value, label: `${date.getMonth() + 1}월 ${date.getDate()}일` };
    });
  if (periodUnit.value === "month")
    return Array.from({ length: 12 }, (_, index) => ({ value: String(index + 1), label: `${index + 1}월` }));
  if (periodUnit.value === "quarter")
    return [1, 2, 3, 4].map((value) => ({ value: String(value), label: `${value}분기 (${(value - 1) * 3 + 1}~${value * 3}월)` }));
  return [currentYear, String(today.getFullYear() - 1), String(today.getFullYear() - 2)].map((value) => ({ value, label: `${value}년` }));
});

function setPeriodUnit(unit) {
  periodUnit.value = unit;
  selectedPeriod.value = unit === "day" ? today.toISOString().slice(0, 10) : unit === "year" ? currentYear : "1";
}

function getDateRange() {
  const year = Number(currentYear);
  if (periodUnit.value === "day") return { from: selectedPeriod.value, to: selectedPeriod.value, year: selectedPeriod.value.slice(0, 4), unit: "day" };
  if (periodUnit.value === "month") {
    const month = String(selectedPeriod.value).padStart(2, "0");
    const lastDay = new Date(year, Number(month), 0).getDate();
    return { from: `${year}-${month}-01`, to: `${year}-${month}-${lastDay}`, year: String(year), unit: "month" };
  }
  if (periodUnit.value === "quarter") {
    const startMonth = (Number(selectedPeriod.value) - 1) * 3 + 1;
    const endMonth = startMonth + 2;
    const lastDay = new Date(year, endMonth, 0).getDate();
    return { from: `${year}-${String(startMonth).padStart(2, "0")}-01`, to: `${year}-${String(endMonth).padStart(2, "0")}-${lastDay}`, year: String(year), unit: "month" };
  }
  return { from: `${selectedPeriod.value}-01-01`, to: `${selectedPeriod.value}-12-31`, year: selectedPeriod.value, unit: "year" };
}

function updateCurrentDateTime() {
  currentDateTime.value = new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(new Date());
}

// 조회 조건을 개발용 REST API에 전달하고 응답이 도착한 뒤 ECharts 옵션을 갱신합니다.
async function applyFilters() {
  if (!selectedCategories.value.length)
    selectedCategories.value = ["DOG", "CAT", "ALL"];
  loading.value = true;
  try {
    const range = getDateRange();
    dashboard.value = await adminDashboardService.getSalesDashboard({
      unit: range.unit,
      year: range.year,
      from: range.from,
      to: range.to,
      categories: selectedCategories.value,
    });
    appliedAt.value = `${new Date().toLocaleTimeString("ko-KR", {
      hour: "2-digit",
      minute: "2-digit",
    })} 조회`;
  } finally {
    loading.value = false;
  }
}

function renderCharts() {
  if (!salesChartElement.value || !categoryChartElement.value) return;
  salesChart ||= init(salesChartElement.value);
  categoryChart ||= init(categoryChartElement.value);

  salesChart.setOption(
    {
      color: ["#a96e42", "#5f3c2a"],
      tooltip: { trigger: "axis" },
      legend: { top: 0, right: 8, textStyle: { color: "#765337" } },
      grid: { top: 46, right: 54, bottom: 34, left: 58 },
      xAxis: {
        type: "category",
        data: activeData.value.labels,
        axisLine: { lineStyle: { color: "#d8c9bc" } },
        axisTick: { show: false },
      },
      yAxis: [
        {
          type: "value",
          name: "매출(백만원)",
          splitLine: { lineStyle: { color: "#f0e8e0" } },
        },
        {
          type: "value",
          name: "주문(건)",
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: "매출액",
          type: "bar",
          data: activeData.value.sales,
          barMaxWidth: 42,
          itemStyle: { color: "#b77949", borderRadius: [5, 5, 0, 0] },
          emphasis: { itemStyle: { color: "#95643d" } },
        },
        {
          name: "주문 건수",
          type: "line",
          yAxisIndex: 1,
          data: activeData.value.orders,
          smooth: true,
          symbolSize: 7,
          lineStyle: { width: 2 },
          itemStyle: { color: "#5f3c2a" },
        },
      ],
    },
    true,
  );

  categoryChart.setOption(
    {
      color: ["#9b653e", "#9eaf7c", "#efd7ad"],
      tooltip: { trigger: "item", formatter: "{b}<br/>{c}원 ({d}%)" },
      legend: {
        bottom: 0,
        left: "center",
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { color: "#765337" },
      },
      series: [
        {
          name: "카테고리 매출",
          type: "pie",
          radius: ["48%", "72%"],
          center: ["50%", "43%"],
          avoidLabelOverlap: true,
          label: { formatter: "{b}\n{d}%", fontWeight: 700 },
          data: visibleCategories.value.map((row) => ({
            name: row.name,
            value: row.sales,
          })),
        },
      ],
    },
    true,
  );
}

function exportCsv() {
  const header = [
    "카테고리",
    "총 매출",
    "주문 건수",
    "평균 주문금액",
    "취소·환불",
    "성장률",
  ];
  const rows = visibleCategories.value.map((row) => [
    row.name,
    row.sales,
    row.orders,
    Math.round(row.sales / row.orders),
    row.refund,
    row.growth,
  ]);
  const csv = `\uFEFF${[header, ...rows].map((row) => row.join(",")).join("\n")}`;
  const url = URL.createObjectURL(
    new Blob([csv], { type: "text/csv;charset=utf-8" }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = `pawcast-sales-${selectedPeriod.value}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function logout() {
  setAccessToken("");
  router.push("/login");
}
async function reforecast() {
  forecastLoading.value = true;
  try {
    const forecastDate = new Date().toISOString().slice(0, 10);
    const result = await adminDashboardService.reforecast(forecastDate);
    dashboard.value = {
      ...dashboard.value,
      forecast: { ...forecast.value, ...result },
    };
    lastTrainingOrderCount.value = Number(totalOrders.value || 0);
    lastTrainingDate.value = new Date().toLocaleString("ko-KR", { hour12: false });
    lastForecastDate.value = new Date().toLocaleDateString("ko-KR");
    localStorage.setItem("pawcast-ai-last-training-orders", String(lastTrainingOrderCount.value));
    localStorage.setItem("pawcast-ai-last-training-date", lastTrainingDate.value);
    localStorage.setItem("pawcast-ai-last-forecast-date", lastForecastDate.value);
  } finally {
    forecastLoading.value = false;
  }
}

watch(dashboard, async () => {
  await nextTick();
  renderCharts();
});

onMounted(async () => {
  updateCurrentDateTime();
  clockTimer = window.setInterval(updateCurrentDateTime, 1000);
  await applyFilters();
  chartResizeObserver = new ResizeObserver(() => {
    salesChart?.resize();
    categoryChart?.resize();
  });
  if (salesChartElement.value)
    chartResizeObserver.observe(salesChartElement.value);
  if (categoryChartElement.value)
    chartResizeObserver.observe(categoryChartElement.value);
});

onBeforeUnmount(() => {
  chartResizeObserver?.disconnect();
  window.clearInterval(clockTimer);
  salesChart?.dispose();
  categoryChart?.dispose();
});
</script>

<template>
  <div class="admin-dashboard">
    <aside class="admin-unified-sidebar">
      <header class="admin-unified-brand"><b>PAWCAST</b><span>관리자 페이지</span></header>
      <nav class="admin-unified-nav">
        <button type="button" @click="router.push('/admin/dashboard')"><span>최종 대시보드</span></button>
        <button type="button" class="active">
          <span>매출 분석</span>
        </button>
        <button type="button" @click="router.push('/admin/orders')"><span>주문·배송 관리</span></button>
        <button type="button" @click="router.push('/admin/products')"><span>상품 관리</span></button>
        <button type="button" @click="router.push('/admin/products/new')">
          <span>상품 등록</span>
        </button>
        <button type="button" @click="router.push('/admin/members')"><span>회원 관리</span></button>
        <button type="button" @click="router.push('/admin/notices')"><span>공지사항 관리</span></button>
        <button type="button" @click="router.push('/admin/qna')"><span>QnA 관리</span></button>
      </nav>
      <button type="button" class="admin-unified-logout" @click="logout">
        로그아웃
      </button>
    </aside>

    <main class="admin-main">
      <header class="admin-page-heading">
        <div>
          <small>SALES ANALYTICS</small>
          <h1>매출 대시보드</h1>
        </div>
        <p>
          {{
            loading
              ? "매출 데이터를 불러오는 중..."
              : `${currentDateTime} · ${appliedAt}`
          }}
        </p>
      </header>

      <section v-if="retrainingRecommended" class="ai-retraining-alert" role="alert">
        <div><b>AI 재학습 권장</b><p>마지막 학습 이후 주문 {{ ordersSinceTraining.toLocaleString() }}건이 추가되었습니다. 최신 판매 패턴을 반영하려면 AI 모델을 재학습해주세요.</p><small>마지막 학습일: {{ lastTrainingDate }}</small></div>
        <button type="button" :disabled="forecastLoading" @click="reforecast">{{ forecastLoading ? '재학습 중...' : 'AI 재학습·재예측' }}</button>
      </section>

      <section class="admin-filter-panel" aria-label="매출 조회 조건">
        <fieldset class="period-filter">
          <legend>조회 단위</legend>
          <label
            v-for="item in [
              { key: 'day', label: '일' },
              { key: 'month', label: '월' },
              { key: 'quarter', label: '분기' },
              { key: 'year', label: '연도' },
            ]"
            :key="item.key"
          >
            <input :checked="periodUnit === item.key" type="radio" :value="item.key" @change="setPeriodUnit(item.key)" />
            <span>{{ item.label }}</span>
          </label>
        </fieldset>
        <label class="select-filter"
          ><span>{{ periodUnit === 'day' ? '조회일' : periodUnit === 'month' ? '조회 월' : periodUnit === 'quarter' ? '조회 분기' : '조회 연도' }}</span
          ><select v-model="selectedPeriod"><option v-for="item in periodOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
        >
        <fieldset class="category-filter">
          <legend>카테고리</legend>
          <label
            ><input
              v-model="selectedCategories"
              type="checkbox"
              value="DOG"
            />강아지</label
          >
          <label
            ><input
              v-model="selectedCategories"
              type="checkbox"
              value="CAT"
            />고양이</label
          >
          <label
            ><input
              v-model="selectedCategories"
              type="checkbox"
              value="ALL"
            />식기·급수</label
          >
        </fieldset>
        <button
          type="button"
          class="filter-submit"
          :disabled="loading"
          @click="applyFilters"
        >
          {{ loading ? "조회 중" : "조회" }}
        </button>
      </section>

      <section class="admin-kpi-grid">
        <article>
          <small>총 매출</small><strong>{{ formatWon(totalSales) }}</strong>
          <p>전년 동기 대비 <b>▲ 12.8%</b></p>
        </article>
        <article>
          <small>주문 건수</small
          ><strong>{{ totalOrders.toLocaleString() }}건</strong>
          <p>전년 동기 대비 <b>▲ 11.4%</b></p>
        </article>
        <article>
          <small>평균 주문금액</small
          ><strong>{{ formatWon(averageOrder) }}</strong>
          <p>전년 동기 대비 <b>▲ 1.3%</b></p>
        </article>
        <article>
          <small>취소·환불</small><strong>{{ formatWon(totalRefund) }}</strong>
          <p>전체 매출 대비 <em>3.0%</em></p>
        </article>
      </section>

      <section class="admin-chart-grid">
        <article class="admin-panel sales-trend-panel">
          <header>
            <div>
              <h2>
                {{
                  periodUnit === "month"
                    ? "월별"
                    : periodUnit === "day"
                      ? "일별"
                      : periodUnit === "quarter"
                      ? "분기별"
                      : "연도별"
                }}
                매출 추이
              </h2>
              <p>막대: 매출액 · 선: 주문 건수</p>
            </div>
            <span>단위: 백만원</span>
          </header>
          <div ref="salesChartElement" class="echart sales-echart" />
        </article>

        <article class="admin-panel category-share-panel">
          <header><h2>카테고리별 매출 비중</h2></header>
          <div ref="categoryChartElement" class="echart category-echart" />
        </article>
      </section>

      <section class="admin-panel ai-forecast-panel">
        <header>
          <div>
            <small>AI SALES FORECAST</small>
            <h2>판매 예측</h2>
          </div>
          <span class="forecast-date">마지막 예측일: {{ lastForecastDate }}</span>
        </header>
        <div class="ai-forecast-grid">
          <div class="forecast-total">
            <small>다음 달 예상 매출</small
            ><strong>{{ formatWon(forecast.predictedSales) }}</strong>
            <p>
              현재 조회 기간 대비 <b>▲ {{ forecast.growthRate }}%</b> 증가 예상
            </p>
          </div>
          <div class="forecast-message">
            <b>AI 판매 인사이트</b>
            <p>{{ forecast.insight }}</p>
          </div>
          <ol>
            <li v-for="product in forecast.topProducts" :key="product.name">
              <span>{{ product.name }}</span
              ><b>+{{ product.growthRate }}%</b>
            </li>
          </ol>
        </div>
      </section>

      <section class="admin-panel sales-summary-panel">
        <header>
          <h2>매출 요약</h2>
          <button type="button" @click="exportCsv">CSV 내보내기</button>
        </header>
        <div class="admin-table-wrap">
          <table>
            <thead>
              <tr>
                <th>카테고리</th>
                <th>총 매출</th>
                <th>주문 건수</th>
                <th>평균 주문금액</th>
                <th>취소·환불</th>
                <th>전년 동기 대비</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in visibleCategories" :key="row.key">
                <th>{{ row.name }}</th>
                <td>{{ formatWon(row.sales) }}</td>
                <td>{{ row.orders.toLocaleString() }}건</td>
                <td>{{ formatWon(row.sales / row.orders) }}</td>
                <td>{{ formatWon(row.refund) }}</td>
                <td class="growth">▲ {{ row.growth }}%</td>
              </tr>
              <tr class="total-row">
                <th>합계</th>
                <td>{{ formatWon(totalSales) }}</td>
                <td>{{ totalOrders.toLocaleString() }}건</td>
                <td>{{ formatWon(averageOrder) }}</td>
                <td>{{ formatWon(totalRefund) }}</td>
                <td class="growth">▲ 12.8%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.admin-dashboard {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  min-height: 100vh;
  background: #f7f5f2;
  color: #3f281d;
  font-family: inherit;
  transition: grid-template-columns 180ms ease-out;
}
.admin-dashboard.collapsed {
  grid-template-columns: 70px minmax(0, 1fr);
}
.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  border-right: 1px solid #eadfd3;
  background: #fffcf7;
  padding: 26px 14px;
  display: flex;
  flex-direction: column;
  transition: padding 180ms ease-out;
}
.forecast-date { display: flex; align-items: center; gap: 8px; color: #7a5d49; font-size: 13px; font-weight: 800; }
.forecast-date input { border: 1px solid #dcc8b4; border-radius: 7px; padding: 7px 9px; background: #fffdf9; color: inherit; font: inherit; }
.product-insights-panel { margin-top: 22px; }
.product-insights-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.product-insights-grid > article { min-height: 132px; border: 1px solid #eee0d2; border-radius: 12px; background: #fffdf9; padding: 17px; }
.product-insights-grid b { display: block; color: #593d2d; margin-bottom: 10px; }
.product-insights-grid ol { display: grid; gap: 8px; margin: 0; padding: 0; list-style: none; }
.product-insights-grid li { display: flex; justify-content: space-between; gap: 9px; color: #765d4b; font-size: 13px; }
.product-insights-grid li span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-insights-grid li strong { color: #9a643d; white-space: nowrap; }
.product-insights-grid p { margin: 0 0 9px; color: #765d4b; font-size: 13px; line-height: 1.65; }
.product-insights-grid small { color: #a38670; font-size: 12px; }
@media (max-width: 900px) { .product-insights-grid { grid-template-columns: 1fr; } .forecast-date { align-items: flex-start; flex-direction: column; } }
.sidebar-toggle {
  position: absolute;
  z-index: 2;
  top: 23px;
  right: -14px;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid #dac8b8;
  border-radius: 50%;
  background: #fff;
  color: #765337;
  font-size: 20px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgb(72 43 27 / 12%);
}
.admin-sidebar.collapsed {
  padding-right: 8px;
  padding-left: 8px;
}
.admin-sidebar.collapsed header {
  padding-right: 0;
  padding-left: 0;
  text-align: center;
}
.admin-sidebar.collapsed header b {
  font-size: 0;
}
.admin-sidebar.collapsed header b::after {
  content: "P";
  font-size: 24px;
}
.admin-sidebar.collapsed header span,
.admin-sidebar.collapsed nav button span {
  display: none;
}
.admin-sidebar.collapsed nav button {
  justify-content: center;
  padding: 0;
  font-size: 17px;
}
.admin-sidebar.collapsed .admin-logout {
  overflow: hidden;
  width: 44px;
  min-width: 44px;
  margin-right: auto;
  margin-left: auto;
  padding: 0;
  font-size: 0;
}
.admin-sidebar.collapsed .admin-logout::after {
  content: "↪";
  font-size: 18px;
}
.admin-sidebar header {
  padding: 0 12px 22px;
  border-bottom: 1px solid #eadfd3;
}
.admin-sidebar header b {
  display: block;
  color: #8d5b39;
  font-size: 24px;
}
.admin-sidebar header span {
  font-size: 11px;
  color: #8e7767;
}
.admin-sidebar nav {
  display: grid;
  gap: 5px;
  margin-top: 20px;
}
.admin-sidebar nav button,
.admin-logout {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  padding: 0 13px;
  color: #5c4638;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
}
.admin-sidebar nav button.active {
  background: #9a633d;
  color: #fff;
}
.admin-logout {
  margin-top: auto;
  border: 1px solid #d8c4b3;
  justify-content: center;
}
.admin-main {
  min-width: 0;
  padding: 24px 26px 50px;
}
.admin-page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 18px;
}
.admin-page-heading small,
.ai-forecast-panel header small {
  color: #a56d42;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.12em;
}
.admin-page-heading h1 {
  margin: 5px 0 0;
  font-size: 29px;
}
.admin-page-heading p {
  margin: 0;
  color: #9a8475;
  font-size: 11px;
}
.admin-filter-panel {
  display: grid;
  grid-template-columns: auto 110px minmax(290px, 1fr) auto 70px;
  align-items: end;
  gap: 14px;
  border: 1px solid #e2d5c8;
  border-radius: 12px;
  background: #fff;
  padding: 14px;
}
.admin-filter-panel fieldset {
  margin: 0;
  border: 0;
  padding: 0;
}
.admin-filter-panel legend,
.select-filter > span,
.date-filter > span {
  display: block;
  margin-bottom: 7px;
  color: #72513a;
  font-size: 10px;
  font-weight: 900;
}
.period-filter {
  display: flex;
}
.period-filter legend {
  width: 100%;
}
.period-filter label {
  position: relative;
}
.period-filter input {
  position: absolute;
  opacity: 0;
}
.period-filter span {
  display: grid;
  place-items: center;
  min-width: 58px;
  height: 36px;
  border: 1px solid #dbc9ba;
  background: #fff;
  font-size: 11px;
  font-weight: 800;
}
.period-filter label:first-of-type span {
  border-radius: 8px 0 0 8px;
}
.period-filter label:last-of-type span {
  border-radius: 0 8px 8px 0;
}
.period-filter input:checked + span {
  border-color: #95643d;
  background: #95643d;
  color: #fff;
}
.select-filter select,
.date-filter input {
  height: 36px;
  border: 1px solid #dbc9ba;
  border-radius: 7px;
  background: #fff;
  padding: 0 9px;
  color: #4b3427;
}
.select-filter {
  display: block;
}
.select-filter select {
  width: 100%;
}
.date-filter {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}
.date-filter > span {
  width: 100%;
  margin: 0;
}
.date-filter input {
  min-width: 130px;
}
.date-filter i {
  font-style: normal;
}
.category-filter {
  display: flex;
  gap: 10px;
  padding-bottom: 8px !important;
}
.category-filter legend {
  width: 100%;
}
.category-filter label {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 800;
}
.category-filter input {
  accent-color: #95643d;
}
.filter-submit {
  height: 36px;
  border: 0;
  border-radius: 7px;
  background: #95643d;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}
.filter-submit:disabled {
  cursor: wait;
  opacity: 0.65;
}
.admin-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin: 12px 0;
}
.admin-kpi-grid article,
.admin-panel {
  border: 1px solid #e1d5ca;
  border-radius: 12px;
  background: #fff;
}
.admin-kpi-grid article {
  padding: 17px 18px;
}
.admin-kpi-grid small {
  color: #79583f;
  font-weight: 900;
}
.admin-kpi-grid strong {
  display: block;
  margin: 8px 0;
  font-size: 23px;
}
.admin-kpi-grid p {
  margin: 0;
  color: #9b8575;
  font-size: 10px;
}
.admin-kpi-grid b,
.growth {
  color: #4b9855;
}
.admin-kpi-grid em {
  color: #d27355;
  font-style: normal;
  font-weight: 900;
}
.admin-chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(270px, 0.8fr);
  gap: 12px;
}
.admin-panel {
  padding: 16px;
}
.admin-panel > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.admin-panel h2 {
  margin: 0;
  font-size: 16px;
}
.admin-panel header p,
.admin-panel header > span {
  margin: 4px 0 0;
  color: #988273;
  font-size: 10px;
}
.echart {
  width: 100%;
}
.sales-echart {
  height: 300px;
}
.category-echart {
  height: 300px;
}
.category-share-panel {
  text-align: center;
}
.ai-forecast-panel {
  margin-top: 12px;
  border-color: #d5c49e;
  background: #fffaf0;
}
.ai-forecast-grid {
  display: grid;
  grid-template-columns: 0.8fr 1.4fr 1fr;
  gap: 12px;
}
.ai-forecast-grid > div,
.ai-forecast-grid ol {
  margin: 0;
  border-radius: 9px;
  background: #fff;
  padding: 14px;
}
.forecast-total strong {
  display: block;
  margin: 7px 0;
  font-size: 22px;
}
.forecast-total p,
.forecast-message p {
  margin: 0;
  color: #806b5b;
  font-size: 11px;
  line-height: 1.6;
}
.forecast-total b,
.ai-forecast-grid ol b {
  color: #4b9855;
}
.forecast-message > b {
  display: block;
  margin-bottom: 7px;
}
.ai-forecast-grid ol {
  display: grid;
  gap: 8px;
  padding-left: 34px;
}
.ai-forecast-grid li {
  padding-left: 2px;
  font-size: 10px;
}
.ai-forecast-grid li b {
  float: right;
}
.sales-summary-panel {
  margin-top: 12px;
}
.sales-summary-panel header button {
  border: 1px solid #cbb39d;
  border-radius: 6px;
  background: #fff;
  padding: 7px 10px;
  color: #765337;
  font-size: 10px;
  font-weight: 800;
}
.admin-table-wrap {
  overflow: auto;
}
.sales-summary-panel table {
  width: 100%;
  border-collapse: collapse;
  font-size: 10px;
}
.sales-summary-panel th,
.sales-summary-panel td {
  border: 1px solid #e2d6ca;
  padding: 9px 10px;
  text-align: center;
  white-space: nowrap;
}
.sales-summary-panel thead {
  background: #f5eadc;
}
.sales-summary-panel tbody th {
  text-align: left;
}
.total-row {
  background: #f8f0e6;
  font-weight: 900;
}
@media (max-width: 1100px) {
  .admin-filter-panel {
    grid-template-columns: 1fr 1fr;
  }
  .category-filter,
  .date-filter {
    grid-column: span 2;
  }
  .admin-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .admin-chart-grid {
    grid-template-columns: 1fr;
  }
  .ai-forecast-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 760px) {
  .admin-dashboard {
    grid-template-columns: 1fr;
  }
  .admin-sidebar {
    position: static;
    width: auto;
    height: auto;
  }
  .admin-sidebar nav {
    grid-template-columns: repeat(2, 1fr);
  }
  .admin-main {
    padding: 18px 12px;
  }
  .admin-filter-panel {
    grid-template-columns: 1fr;
  }
  .category-filter,
  .date-filter {
    grid-column: auto;
  }
  .admin-kpi-grid {
    grid-template-columns: 1fr;
  }
  .admin-page-heading {
    align-items: flex-start;
    gap: 8px;
    flex-direction: column;
  }
  .ai-forecast-grid {
    grid-template-columns: 1fr;
  }
}
.admin-sidebar nav button,
.admin-logout,
.sidebar-toggle,
.admin-filter-panel button,
.sales-summary-panel header button {
  transition:
    color 180ms ease-out,
    background-color 180ms ease-out,
    border-color 180ms ease-out,
    box-shadow 180ms ease-out;
}
.admin-sidebar nav button:hover:not(.active),
.sidebar-toggle:hover,
.sales-summary-panel header button:hover {
  border-color: #d7b892;
  background: #fffbf1;
  color: #8b5e3c;
}
.admin-filter-panel > button:hover,
.admin-logout:hover {
  background: #74492e;
}
.admin-filter-panel > button:active,
.admin-logout:active {
  background: #603a25;
}
.admin-dashboard :is(a, button, input, select):focus-visible {
  outline: 2px solid #d59a3a;
  outline-offset: 2px;
}
</style>

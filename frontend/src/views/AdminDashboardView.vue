<!-- 판매자·관리자가 실제 기간별 매출 실적과 AI 판매 예측을 검수하는 관리자 대시보드입니다. -->
<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { adminDashboardService } from "@/services/adminDashboardService";
import AdminSidebar from "@/components/AdminSidebar.vue";

// # ECharts는 매출 화면에서 차트를 그릴 때만 내려받아 초기 관리자 번들을 줄인다.
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

// # 기본은 월별 흐름으로 보여 주고, 일별 조회는 선택한 날짜 범위에만 사용한다.
const loginAt = new Date(sessionStorage.getItem("powcast-login-at") || Date.now());
const loginYear = loginAt.getFullYear();
const toIsoDate = (date) => date.toISOString().slice(0, 10);
const periodUnit = ref("month");
const selectedYear = ref(String(loginYear));
const selectedMonth = ref(String(loginAt.getMonth() + 1));
const selectedQuarter = ref(String(Math.floor(loginAt.getMonth() / 3) + 1));
const availableYears = ref([]);
const fromDate = ref(`${loginYear}-01-01`);
const toDate = ref(`${loginYear}-12-31`);
const selectedCategories = ref(["DOG", "CAT", "ALL"]);
const appliedAt = ref("초기 화면 데이터");
const dashboard = ref(null);
const loading = ref(false);
const error = ref("");
const retraining = ref(false);
const retrainMessage = ref("");
const exportFormat = ref("csv");
const exportSection = ref("all");
const exporting = ref(false);
const exportMessage = ref("");
const salesChartElement = ref(null);
const categoryChartElement = ref(null);
let salesChart;
let categoryChart;
let chartResizeObserver;

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
const visibleCategories = computed(() =>
  (dashboard.value?.categories || []).map((row) => ({
    ...row,
    orders: Number(row.orders || 0),
    refund: Number(row.refund || 0),
    growth: Number(row.growth || 0),
  })),
);
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

const formatWon = (value) => `${Math.round(value).toLocaleString()}원`;
const loginTimestamp = computed(() => new Intl.DateTimeFormat("ko-KR", { year:"numeric", month:"2-digit", day:"2-digit", weekday:"short", hour:"2-digit", minute:"2-digit", second:"2-digit", hour12:false }).format(loginAt));

function syncPeriodRange() {
  const year = Number(selectedYear.value || loginYear);
  if (periodUnit.value === "day") {
    fromDate.value = toIsoDate(loginAt);
    toDate.value = fromDate.value;
  } else if (periodUnit.value === "month") {
    const month = Number(selectedMonth.value);
    fromDate.value = `${year}-${String(month).padStart(2, "0")}-01`;
    toDate.value = toIsoDate(new Date(year, month, 0));
  } else if (periodUnit.value === "quarter") {
    const month = (Number(selectedQuarter.value) - 1) * 3;
    fromDate.value = toIsoDate(new Date(year, month, 1));
    toDate.value = toIsoDate(new Date(year, month + 3, 0));
  } else {
    const years = availableYears.value.length ? availableYears.value : [year];
    fromDate.value = `${Math.min(...years)}-01-01`;
    toDate.value = `${Math.max(...years)}-12-31`;
  }
}

// # 조회 조건을 실제 매출 API에 전달하고 응답이 도착한 뒤 ECharts 옵션을 갱신한다.
async function applyFilters() {
  if (!selectedCategories.value.length)
    selectedCategories.value = ["DOG", "CAT", "ALL"];
  loading.value = true;
  error.value = "";
  try {
    dashboard.value = await adminDashboardService.getSalesDashboard({
      unit: periodUnit.value,
      year: selectedYear.value,
      from: fromDate.value,
      to: toDate.value,
      categories: selectedCategories.value,
    });
    appliedAt.value = `${new Date().toLocaleTimeString("ko-KR", {
      hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false,
    })} 조회`;
  } catch (cause) {
    error.value = cause.message || "매출 데이터를 불러오지 못했습니다.";
    // # API 실패 시 임시 매출 수치를 만들지 않고 빈 차트와 오류 안내를 표시한다.
    dashboard.value = null;
  } finally {
    loading.value = false;
  }
}

async function retrainModel() {
  retraining.value = true;
  retrainMessage.value = "";
  try {
    const result = await adminDashboardService.retrainSalesModel(
      new Date().toISOString().slice(0, 10),
    );
    retrainMessage.value = `재학습 완료 · ${result.trainingDays || 0}일 주문 데이터를 반영했습니다.`;
    await applyFilters();
  } catch (cause) {
    retrainMessage.value = `재학습 요청 실패: ${cause.message || "AI 서버 연결을 확인해주세요."}`;
  } finally {
    retraining.value = false;
  }
}

async function renderCharts() {
  if (!salesChartElement.value || !categoryChartElement.value) return;
  const init = await loadEcharts();
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

async function downloadStatistics() {
  exporting.value = true;
  exportMessage.value = "";
  try {
    await adminDashboardService.downloadStatistics({
      startDate: fromDate.value,
      endDate: toDate.value,
      format: exportFormat.value,
      section: exportSection.value,
    });
    exportMessage.value = "실제 DB 통계 파일을 생성했습니다.";
  } catch (cause) {
    exportMessage.value = cause.message || "통계 파일 다운로드에 실패했습니다.";
  } finally {
    exporting.value = false;
  }
}

watch(dashboard, async () => {
  await nextTick();
  await renderCharts();
});

onMounted(async () => {
  try {
    const years = await adminDashboardService.getSalesYears();
    availableYears.value = Array.isArray(years) && years.length ? years : [loginYear];
    if (!availableYears.value.includes(loginYear)) availableYears.value.unshift(loginYear);
    selectedYear.value = String(availableYears.value[0]);
  } catch {
    availableYears.value = [loginYear];
  }
  syncPeriodRange();
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

watch([periodUnit, selectedYear, selectedMonth, selectedQuarter], syncPeriodRange);

onBeforeUnmount(() => {
  chartResizeObserver?.disconnect();
  salesChart?.dispose();
  categoryChart?.dispose();
});
</script>

<template>
  <div class="admin-dashboard">
    <AdminSidebar />

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
              : `로그인 ${loginTimestamp} · ${appliedAt}`
          }}
        </p>
      </header>

      <section class="admin-filter-panel" aria-label="매출 조회 조건">
        <fieldset class="period-filter">
          <legend>조회 단위</legend>
          <label
            v-for="item in [
              { key: 'day', label: '일별' },
              { key: 'month', label: '월별' },
              { key: 'quarter', label: '분기별' },
              { key: 'year', label: '연도별' },
            ]"
            :key="item.key"
          >
            <input v-model="periodUnit" type="radio" :value="item.key" />
            <span>{{ item.label }}</span>
          </label>
        </fieldset>
        <label v-if="periodUnit === 'month' || periodUnit === 'quarter'" class="select-filter"><span>기준 연도</span><select v-model="selectedYear"><option v-for="year in availableYears" :key="year" :value="String(year)">{{ year }}년</option></select></label>
        <label v-if="periodUnit === 'month'" class="select-filter"><span>조회 월</span><select v-model="selectedMonth"><option v-for="month in 12" :key="month" :value="String(month)">{{ month }}월</option></select></label>
        <label v-if="periodUnit === 'quarter'" class="select-filter"><span>조회 분기</span><select v-model="selectedQuarter"><option v-for="quarter in 4" :key="quarter" :value="String(quarter)">{{ quarter }}분기</option></select></label>
        <div v-if="periodUnit === 'year'" class="available-year-list"><span>비교 연도</span><b v-for="year in availableYears" :key="year">{{ year }}</b></div>
        <div class="date-filter"><span>{{ periodUnit === 'day' ? '로그인 기준 일자' : '조회 기간' }}</span><b>{{ fromDate }}</b><i v-if="periodUnit !== 'day'">~</i><b v-if="periodUnit !== 'day'">{{ toDate }}</b></div>
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
          <p>선택 조회 기간 기준</p>
        </article>
        <article>
          <small>주문 건수</small
          ><strong>{{ totalOrders.toLocaleString() }}건</strong>
          <p>선택 조회 기간 기준</p>
        </article>
        <article>
          <small>평균 주문금액</small
          ><strong>{{ formatWon(averageOrder) }}</strong>
          <p>선택 조회 기간 기준</p>
        </article>
        <article>
          <small>취소·환불</small><strong>{{ formatWon(totalRefund) }}</strong>
          <p>취소·환불 주문 합계</p>
        </article>
      </section>
      <p v-if="error" class="dashboard-error">{{ error }}</p>

      <section class="admin-panel export-panel">
        <header>
          <div>
            <small>DATABASE EXPORT</small>
            <h2>분석·예측 데이터 다운로드</h2>
            <p>
              화면값이 아니라 선택 기간의 실제 DB 테이블을 Python에서 다시
              조회합니다.
            </p>
          </div>
        </header>
        <div class="export-controls">
          <label>
            <span>포함 데이터</span>
            <select v-model="exportSection">
              <option value="all">전체 통계</option>
              <option value="summary">매출 요약</option>
              <option value="categories">카테고리 통계</option>
              <option value="daily">일별 매출</option>
              <option value="products">상품·재고 통계</option>
              <option value="reviews">리뷰 감정 통계</option>
              <option value="forecast">판매 예측</option>
            </select>
          </label>
          <label>
            <span>파일 형식</span>
            <select v-model="exportFormat">
              <option value="csv">CSV</option>
              <option value="xlsx">Excel</option>
            </select>
          </label>
          <button
            type="button"
            :disabled="exporting"
            @click="downloadStatistics"
          >
            {{ exporting ? "파일 생성 중" : "DB 통계 다운로드" }}
          </button>
        </div>
        <p v-if="exportMessage" class="export-message">{{ exportMessage }}</p>
      </section>

      <section class="admin-chart-grid">
        <article class="admin-panel sales-trend-panel">
          <header>
            <div>
              <h2>
                {{
                  periodUnit === "day"
                    ? "일별"
                    : periodUnit === "month"
                      ? "월별"
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
            <h2>다음 달 판매 예측</h2>
          </div>
          <button type="button" :disabled="retraining" @click="retrainModel">
            {{ retraining ? "AI 재학습 중" : "AI 모델 재학습" }}
          </button>
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
        <p v-if="retrainMessage" class="retrain-message">
          {{ retrainMessage }}
        </p>
      </section>

      <section class="admin-panel sales-summary-panel">
        <header>
          <h2>매출 요약</h2>
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
                <td>{{ formatWon(row.orders ? row.sales / row.orders : 0) }}</td>
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
  color: #3f281d;
  font-family: inherit;
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
  transition: padding 0.22s ease;
}
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
.date-filter b { display: inline-flex; align-items: center; min-height: 38px; border: 1px solid #decbbb; border-radius: 7px; padding: 0 11px; background: #fffdfb; color: #5c4030; font-size: 13px; font-weight: 700; }
.available-year-list { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.available-year-list > span { width: 100%; color: #7a5a42; font-size: 11px; font-weight: 800; }
.available-year-list b { border-radius: 999px; background: #f1e5d8; padding: 6px 9px; color: #865737; font-size: 12px; }
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
.export-panel {
  margin-bottom: 12px;
  background: #fffaf3;
}
.export-panel header p {
  margin: 5px 0 0;
  color: #8f7969;
  font-size: 11px;
}
.export-controls {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 140px auto;
  align-items: end;
  gap: 12px;
}
.export-controls label span {
  display: block;
  margin-bottom: 6px;
  color: #72513a;
  font-size: 10px;
  font-weight: 900;
}
.export-controls select,
.export-controls button {
  width: 100%;
  height: 38px;
  border: 1px solid #d8c4b3;
  border-radius: 7px;
  background: #fff;
  padding: 0 10px;
  color: #4b3427;
}
.export-controls button {
  border: 0;
  background: #95643d;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}
.export-controls button:disabled {
  cursor: wait;
  opacity: 0.65;
}
.export-message {
  margin: 10px 0 0;
  color: #765337;
  font-size: 11px;
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
  .export-controls {
    grid-template-columns: 1fr;
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
</style>

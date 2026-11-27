
<!-- 관리자 매출 분석: FastAPI가 PostgreSQL을 직접 집계한 응답 기반 -->
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
import { isAdminRole, readUserRole } from "@/utils/roles";
import "@/styles/admin-sales-analytics.css";
import salesExportSchema from "@shared/export-schemas/sales.json";

const isAdmin = isAdminRole(readUserRole());
const today = new Date();
const currentYear = today.getFullYear();
const currentMonth = today.getMonth() + 1;

const periodUnit = ref("month");
const selectedYear = ref(String(currentYear));
const selectedMonth = ref(String(currentMonth));
const selectedQuarter = ref(String(Math.ceil(currentMonth / 3)));
const selectedDay = ref(formatDate(today));

const availableYears = ref([currentYear]);
const dashboard = ref(null);

const loading = ref(false);
const error = ref("");
const appliedAt = ref("");

const retraining = ref(false);
const retrainMessage = ref("");

const exportSection = ref("all");
const exportFormat = ref("csv");
const exporting = ref(false);
const exportMessage = ref("");
const exportSections = salesExportSchema.sections;
const exportFormats = salesExportSchema.formats;

const salesChartElement = ref(null);
const categoryChartElement = ref(null);

let salesChart = null;
let categoryChart = null;
let resizeObserver = null;
let refreshSalesBreakMarks = null;
let echartsModule = null;
let chartRenderSequence = 0;

const appliedFilters = ref({
  unit: "month",
  year: String(currentYear),
  from: `${currentYear}-${String(currentMonth).padStart(2, "0")}-01`,
  to: formatDate(new Date(currentYear, currentMonth, 0)),
});

const periodOptions = [
  { value: "day", label: "일" },
  { value: "month", label: "월" },
  { value: "quarter", label: "분기" },
  { value: "year", label: "연도" },
];

const categoryDefinitions = [
  {
    name: "강아지관",
    color: "#ad6d43",
    aliases: ["강아지관", "강아지", "DOG"],
  },
  {
    name: "고양이관",
    color: "#829568",
    aliases: ["고양이관", "고양이", "CAT"],
  },
  {
    name: "식기·급수관",
    color: "#7190a4",
    aliases: ["식기·급수관", "식기급수관", "DEVICE"],
  },
];

const summary = computed(() => ({
  totalSales: Number(dashboard.value?.summary?.totalSales ?? 0),
  totalOrders: Number(dashboard.value?.summary?.totalOrders ?? 0),
  averageOrderAmount: Number(
    dashboard.value?.summary?.averageOrderAmount ?? 0,
  ),
  refundAmount: Number(dashboard.value?.summary?.refundAmount ?? 0),
}));

const categories = computed(() =>
  (dashboard.value?.categories ?? []).map((item, index) => ({
    ...item,
    key: item.key ?? `${item.name}-${index}`,
    sales: Number(item.sales ?? 0),
    orders: item.orders == null ? null : Number(item.orders),
    refund: item.refund == null ? null : Number(item.refund),
    growth: item.growth == null ? null : Number(item.growth),
  })),
);

const categoryComparison = computed(() =>
  categoryDefinitions.map((definition) => {
    const matched = categories.value.find((item) =>
      definition.aliases.includes(item.name),
    );

    return {
      name: definition.name,
      color: definition.color,
      sales: matched ? matched.sales : null,
      available: Boolean(matched),
    };
  }),
);

const availableCategoryComparison = computed(() =>
  categoryComparison.value.filter((item) => item.available),
);

const comparisonCategoryHasData = computed(() =>
  categoryComparison.value.some(
    (item) => item.available && (item.name === "강아지관" || item.name === "고양이관"),
  ),
);

const categoryMaximum = computed(() => {
  if (!availableCategoryComparison.value.length) {
    return null;
  }

  return availableCategoryComparison.value.reduce((maximum, item) =>
    item.sales > maximum.sales ? item : maximum,
  );
});

const categoryMinimum = computed(() => {
  if (!availableCategoryComparison.value.length) {
    return null;
  }

  return availableCategoryComparison.value.reduce((minimum, item) =>
    item.sales < minimum.sales ? item : minimum,
  );
});

const forecast = computed(() => ({
  predictedSales:
    dashboard.value?.forecast?.predictedSales == null
      ? null
      : Number(dashboard.value.forecast.predictedSales),
  growthRate: dashboard.value?.forecast?.growthRate ?? null,
  forecastBaseDate: dashboard.value?.forecast?.forecastBaseDate || "",
  targetMonth: dashboard.value?.forecast?.targetMonth || "",
  forecastStartDate: dashboard.value?.forecast?.forecastStartDate || "",
  forecastEndDate: dashboard.value?.forecast?.forecastEndDate || "",
  forecastDays: Number(dashboard.value?.forecast?.forecastDays ?? 0),
  isCumulative: dashboard.value?.forecast?.isCumulative === true,
  growthComparison: dashboard.value?.forecast?.growthComparison || "",
  previousDailyAverage: Number(
    dashboard.value?.forecast?.previousDailyAverage ?? 0,
  ),
  recentDailyAverage: Number(
    dashboard.value?.forecast?.recentDailyAverage ?? 0,
  ),
  insight:
    dashboard.value?.forecast?.insight ||
    "제공된 판매 인사이트가 없습니다.",
  topProducts: dashboard.value?.forecast?.topProducts ?? [],
}));

const totalCategorySales = computed(() =>
  categories.value.reduce((sum, category) => sum + category.sales, 0),
);

const kpiCards = computed(() => [
  {
    label: "총 매출",
    value: formatWon(summary.value.totalSales),
    description: "선택 기간 매출 합계",
  },
  {
    label: "주문 건수",
    value: `${formatNumber(summary.value.totalOrders)}건`,
    description: "선택 기간 주문 합계",
  },
  {
    label: "평균 주문금액",
    value: formatWon(summary.value.averageOrderAmount),
    description: "주문당 평균 결제 금액",
  },
  {
    label: "취소·환불",
    value: formatWon(summary.value.refundAmount),
    description: "선택 기간 취소·환불 금액",
  },
]);

const comparisonPeriod = computed(() => {
  const { unit, year, from, to } = appliedFilters.value;

  if (unit === "day") {
    return from;
  }

  if (unit === "month") {
    return `${year}년 ${Number(from.slice(5, 7))}월`;
  }

  if (unit === "quarter") {
    const quarter = Math.ceil(Number(from.slice(5, 7)) / 3);
    return `${year}년 ${quarter}분기`;
  }

  return `${year}년 전체`;
});

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function getForecastDate() {
  // FastAPI 계약에서 기준일은 학습에 포함되는 마지막 날(오늘)입니다.
  return formatDate(new Date());
}

function formatDateTime(date) {
  const weekdays = ["일", "월", "화", "수", "목", "금", "토"];

  const time = [
    date.getHours(),
    date.getMinutes(),
    date.getSeconds(),
  ]
    .map((value) => String(value).padStart(2, "0"))
    .join(":");

  return `${formatDate(date)} (${weekdays[date.getDay()]}) ${time}`;
}

function formatNumber(value) {
  return Number(value ?? 0).toLocaleString("ko-KR");
}

function formatWon(value) {
  if (value == null) {
    return "-";
  }

  return `${formatNumber(Math.round(Number(value)))}원`;
}

function formatPercent(value) {
  if (value == null || !Number.isFinite(Number(value))) {
    return "-";
  }

  const number = Number(value);

  return `${number > 0 ? "+" : ""}${number}%`;
}

function formatAxisWon(value) {
  return `${formatNumber(Math.round(Number(value) / 10000))}만`;
}

function buildFilters() {
  const year = Number(selectedYear.value);

  let from;
  let to;

  if (periodUnit.value === "day") {
    from = selectedDay.value;
    to = selectedDay.value;
  } else if (periodUnit.value === "month") {
    const month = Number(selectedMonth.value);

    from = formatDate(new Date(year, month - 1, 1));
    to = formatDate(new Date(year, month, 0));
  } else if (periodUnit.value === "quarter") {
    const firstMonth = (Number(selectedQuarter.value) - 1) * 3;

    from = formatDate(new Date(year, firstMonth, 1));
    to = formatDate(new Date(year, firstMonth + 3, 0));
  } else {
    from = `${year}-01-01`;
    to = `${year}-12-31`;
  }

  return {
    unit: periodUnit.value,
    year: String(year),
    from,
    to,
  };
}

async function loadDashboard() {
  if (loading.value) {
    return;
  }

  loading.value = true;
  error.value = "";

  const filters = buildFilters();
  const forecastDate = getForecastDate();

  try {
    const result = await adminDashboardService.getSalesDashboard(
      filters,
      forecastDate,
    );
    // Vue가 관리자 JWT를 전달해 FastAPI의 실제 PostgreSQL 집계를 직접 조회한다.
    dashboard.value = {
      ...result,
      forecast: {
        ...(result.forecast || {}),
        topProducts: result.forecast?.topProducts || [],
      },
    };
    appliedFilters.value = filters;
    appliedAt.value = formatDateTime(new Date());
  } catch (cause) {
    error.value =
      cause.message || "매출 데이터를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

async function retrainModel() {
  if (retraining.value) {
    return;
  }

  retraining.value = true;
  retrainMessage.value = "";

  try {
    const result = await adminDashboardService.retrainSalesModel(
      getForecastDate(),
    );

    // POST 응답이 이번 재학습의 최종 결과입니다. 전체 dashboard를 다시 조회하면
    // 선택 기간 기준 예측이 다시 내려와 방금 계산한 예측값을 덮을 수 있습니다.
    const trainedProducts = Array.isArray(result?.top_products)
      ? result.top_products
      : [];
    dashboard.value = {
      ...(dashboard.value || {}),
      forecast: {
        predictedSales:
          result?.predicted_sales == null
            ? null
            : Number(result.predicted_sales),
        growthRate:
          result?.growth_rate == null
            ? null
            : Number(result.growth_rate),
        forecastBaseDate:
          result?.forecastBaseDate || result?.forecast_date || getForecastDate(),
        insight:
          result?.insight || "재학습 응답에 판매 인사이트가 없습니다.",
        forecastDate: result?.forecast_date || getForecastDate(),
        trainingDays: Number(result?.training_days ?? 0),
        targetMonth: result?.targetMonth || "",
        forecastStartDate: result?.forecastStartDate || "",
        forecastEndDate: result?.forecastEndDate || "",
        forecastDays: Number(result?.forecastDays ?? 0),
        isCumulative: result?.isCumulative === true,
        growthComparison:
          result?.growthComparison || "",
        previousDailyAverage: Number(result?.previousDailyAverage ?? 0),
        recentDailyAverage: Number(result?.recentDailyAverage ?? 0),
        topProducts: trainedProducts.map((product) => ({
          name: product.name || "이름 없는 상품",
          sales: Number(product.sales ?? 0),
          quantity: Number(product.quantity ?? 0),
          growthRate: Number(product.growthRate ?? 0),
        })),
      },
    };
    appliedAt.value = formatDateTime(new Date());
    retrainMessage.value =
      "재학습 완료 · 최근 " +
      Number(result?.training_days ?? 0) +
      "일 데이터 반영";
  } catch (cause) {
    retrainMessage.value =
      cause.message || "재학습 요청에 실패했습니다.";
  } finally {
    retraining.value = false;
  }
}

async function downloadStatistics() {
  if (exporting.value) {
    return;
  }

  exporting.value = true;
  exportMessage.value = "";

  try {
    await adminDashboardService.downloadStatistics({
      startDate: appliedFilters.value.from,
      endDate: appliedFilters.value.to,
      format: exportFormat.value,
      section: exportSection.value,
    });

    exportMessage.value = "다운로드를 요청했습니다.";
  } catch (cause) {
    exportMessage.value =
      cause.message || "통계 파일 다운로드에 실패했습니다.";
  } finally {
    exporting.value = false;
  }
}

async function loadEcharts() {
  if (echartsModule) {
    return echartsModule;
  }

  const [core, charts, components, renderers] = await Promise.all([
    import("echarts/core"),
    import("echarts/charts"),
    import("echarts/components"),
    import("echarts/renderers"),
  ]);

  core.use([
    charts.BarChart,
    charts.PieChart,
    components.GridComponent,
    components.LegendComponent,
    components.TooltipComponent,
    renderers.CanvasRenderer,
  ]);

  echartsModule = core;

  return core;
}

async function renderCharts() {
  const sequence = ++chartRenderSequence;

  await nextTick();

  if (!salesChartElement.value || !categoryChartElement.value) {
    return;
  }

  const echarts = await loadEcharts();

  if (
    sequence !== chartRenderSequence ||
    !salesChartElement.value ||
    !categoryChartElement.value
  ) {
    return;
  }

  if (!salesChart) {
    salesChart = echarts.init(salesChartElement.value);
  }

  if (!categoryChart) {
    categoryChart = echarts.init(categoryChartElement.value);
  }

  // 막대그래프는 강아지관·고양이관만 비교합니다.
  const comparison = categoryComparison.value.filter(
    (item) => item.name === "강아지관" || item.name === "고양이관",
  );
  const validSales = comparison
    .filter((item) => item.available && Number.isFinite(item.sales) && item.sales >= 0)
    .map((item) => item.sales);
  const minimumSales = validSales.length ? Math.min(...validSales) : 0;
  const maximumSales = validSales.length ? Math.max(...validSales) : 0;
  // 0원 시작을 생략하되 실제 매출액과 축 시작값을 모두 명확히 표시합니다.
  // 차이가 없거나 0원인 경우에는 왜곡을 피하기 위해 일반 축을 사용합니다.
  const useAxisBreak = validSales.length === 2 && minimumSales > 0 && maximumSales > minimumSales;
  const proposedMinimum = useAxisBreak
    ? Math.max(0, minimumSales - (maximumSales - minimumSales) * 0.28)
    : 0;
  const proposedMaximum = Math.max(
    100000,
    maximumSales + Math.max((maximumSales - proposedMinimum) * 0.22, 100000),
  );
  // 1·2·5 단위의 일정한 눈금 간격을 선택합니다. 축 양끝도 동일 간격에 맞춥니다.
  const proposedStep = Math.max((proposedMaximum - proposedMinimum) / 4, 1);
  const magnitude = 10 ** Math.floor(Math.log10(proposedStep));
  const axisInterval = [1, 2, 5, 10]
    .map((unit) => unit * magnitude)
    .find((step) => step >= proposedStep) ?? 10 * magnitude;
  const axisMinimum = useAxisBreak
    ? Math.floor(proposedMinimum / axisInterval) * axisInterval
    : 0;
  const axisMaximum = Math.max(
    axisMinimum + axisInterval,
    Math.ceil(proposedMaximum / axisInterval) * axisInterval,
  );
  const barWidth = 104;

  salesChart.setOption(
    {
      animationDuration: 450,
      tooltip: {
        trigger: "axis",
        axisPointer: { type: "none" },
        formatter(params) {
          const name = params[0]?.axisValue;
          const item = comparison.find((entry) => entry.name === name);
          return item?.available
            ? `<strong>${item.name}</strong><br/>실제 매출: ${formatWon(item.sales)}`
            : `${name ?? ""}<br/>매출 데이터 없음`;
        },
      },
      grid: {
        left: 92,
        right: 90,
        top: 72,
        bottom: 72,
        containLabel: true,
      },
      xAxis: {
        type: "category",
        data: comparison.map((item) => item.name),
        axisLine: { lineStyle: { color: "#bda996" } },
        axisTick: { show: false },
        axisLabel: {
          color: "#493326",
          fontSize: 17,
          fontWeight: 800,
          margin: 18,
        },
      },
      yAxis: {
        type: "value",
        min: axisMinimum,
        max: axisMaximum,
        interval: axisInterval,
        splitNumber: 4,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          color: "#604938",
          fontSize: 13,
          fontWeight: 700,
          formatter: formatAxisWon,
        },
        splitLine: { lineStyle: { color: "#eee4dc", type: "dashed" } },
      },
      series: [
        {
          name: "카테고리 매출",
          type: "bar",
          barWidth,
          data: comparison.map((item) => ({
            value: item.available ? item.sales : null,
            itemStyle: {
              color: item.color,
              borderRadius: [7, 7, 0, 0],
            },
          })),
          label: {
            show: true,
            position: "top",
            distance: 12,
            color: "#493326",
            fontSize: 15,
            fontWeight: 800,
            formatter(params) {
              const item = comparison[params.dataIndex];
              return item?.available ? formatWon(item.sales) : "";
            },
          },
        },
      ],
    },
    true,
  );

  // 생략 지점에 두 줄의 지그재그 물결을 올려 막대가 0원부터 시작하지 않음을 명시합니다.
  // ECharts graphic은 차트 좌표를 기준으로 계산하여 창 크기 변경에도 재배치합니다.
  function updateBreakMarks() {
    if (!salesChart || !useAxisBreak) {
      salesChart?.setOption({ graphic: [] });
      return;
    }
    const elements = comparison.flatMap((item, index) => {
      if (!item.available) return [];
      const centerX = salesChart.convertToPixel({ xAxisIndex: 0 }, index);
      const baselineY = salesChart.convertToPixel({ yAxisIndex: 0 }, axisMinimum);
      if (!Number.isFinite(centerX) || !Number.isFinite(baselineY)) return [];
      const left = centerX - barWidth / 2 + 5;
      const points = Array.from({ length: 13 }, (_, i) => [
        left + i * (barWidth - 10) / 12,
        baselineY - 19 + (i % 2 ? -6 : 4),
      ]);
      return [{
        id: `sales-axis-break-${index}`,
        type: "polyline",
        silent: true,
        z: 100,
        shape: { points },
        style: { stroke: "#ffffff", lineWidth: 5, fill: null, lineJoin: "round" },
      }];
    });
    salesChart.setOption({ graphic: elements });
  }
  // 초기 레이아웃 확정 이후에도 정확한 위치에 물결을 표시합니다.
  salesChart.resize();
  updateBreakMarks();
  // 리사이즈 때의 물결 위치는 ResizeObserver에서도 갱신합니다.
  refreshSalesBreakMarks = updateBreakMarks;

  // 실제 매출 비율을 유지하면서 관 이름과 비율을 도넛 바깥에 표시합니다.
  const donutCategories = categoryComparison.value
    .filter((item) => item.available)
    .sort((a, b) => (a.name === "식기·급수관" ? -1 : b.name === "식기·급수관" ? 1 : 0));

  categoryChart.setOption(
    {
      color: donutCategories.map((item) => item.color),
      tooltip: {
        trigger: "item",
        formatter: (item) =>
          `${item.name}: ${formatWon(item.value)} (${item.percent}%)`,
      },
      series: [
        {
          type: "pie",
          radius: ["38%", "59%"],
          center: ["50%", "50%"],
          avoidLabelOverlap: true,
          minShowLabelAngle: 0,
          labelLayout: {
            hideOverlap: false,
            moveOverlap: "shiftY",
          },
          labelLine: {
            show: true,
            length: 13,
            length2: 12,
            smooth: 0.15,
            lineStyle: { color: "#806b59", width: 1.6 },
          },
          label: {
            show: true,
            position: "outside",
            color: "#493326",
            fontSize: 13,
            fontWeight: 800,
            alignTo: "none",
            edgeDistance: "8%",
            formatter: (params) => `${params.name}\n${params.percent.toFixed(1)}%`,
            lineHeight: 19,
          },
          data: donutCategories.map((item) => ({
            name: item.name,
            value: item.sales,
            label: { show: item.sales > 0 },
            labelLine: { show: item.sales > 0 },
            itemStyle: {
              borderColor: "#fff",
              borderWidth: 2,
            },
          })),
        },
      ],
    },
    true,
  );

  salesChart.resize();
  categoryChart.resize();
}

watch(dashboard, () => {
  renderCharts().catch((cause) => {
    error.value =
      cause.message || "차트를 표시하지 못했습니다.";
  });
});

onMounted(async () => {
  try {
    const years = await adminDashboardService.getSalesYears();

    if (Array.isArray(years) && years.length) {
      availableYears.value = [
        ...new Set([...years.map(Number), currentYear]),
      ].sort((a, b) => b - a);

      // 올해 주문이 아직 없으면 화면이 모두 0원으로 보이는 것을 피하고,
      // DB에 있는 최신 연도의 전체 매출을 첫 화면에 표시한다.
      if (!years.map(Number).includes(currentYear)) {
        selectedYear.value = String(availableYears.value[0]);
        periodUnit.value = "year";
      }
    }
  } catch {
    availableYears.value = [currentYear];
  }

  await loadDashboard();

  if (typeof ResizeObserver !== "undefined") {
    resizeObserver = new ResizeObserver(() => {
      salesChart?.resize();
      refreshSalesBreakMarks?.();
      categoryChart?.resize();
    });

    if (salesChartElement.value) {
      resizeObserver.observe(salesChartElement.value);
    }

    if (categoryChartElement.value) {
      resizeObserver.observe(categoryChartElement.value);
    }
  }
});

onBeforeUnmount(() => {
  chartRenderSequence++;

  resizeObserver?.disconnect();

  salesChart?.dispose();
  categoryChart?.dispose();

  salesChart = null;
  categoryChart = null;
});
</script>

<template>
  <main class="sales-analytics">
    <header class="sales-page-header">
      <section>
        <h1>매출 분석</h1>
        <p>일 · 월 · 분기 · 연도 단위 조회와 판매 예측</p>
      </section>

      <time
        v-if="appliedAt"
        class="sales-last-updated"
      >
      {{ appliedAt }}
      </time>
    </header>

    <section
      class="sales-filters"
      aria-label="매출 조회 조건"
    >
      <fieldset class="sales-period-options">
        <legend class="sr-only">
          조회 단위
        </legend>

        <label
          v-for="option in periodOptions"
          :key="option.value"
        >
          <input
            v-model="periodUnit"
            type="radio"
            :value="option.value"
          />

          <span>
            {{ option.label }}
          </span>
        </label>
      </fieldset>

      <label class="sales-year-filter">
        <span class="sr-only">
          조회 연도
        </span>

        <select v-model="selectedYear">
          <option
            v-for="year in availableYears"
            :key="year"
            :value="String(year)"
          >
            {{ year }}년
          </option>
        </select>
      </label>

      <label
        v-if="periodUnit === 'day'"
        class="sales-period-filter"
      >
        <span class="sr-only">
          조회 날짜
        </span>

        <input
          v-model="selectedDay"
          type="date"
        />
      </label>

      <label
        v-else-if="periodUnit === 'month'"
        class="sales-period-filter"
      >
        <span class="sr-only">
          조회 월
        </span>

        <select v-model="selectedMonth">
          <option
            v-for="month in 12"
            :key="month"
            :value="String(month)"
          >
            {{ month }}월
          </option>
        </select>
      </label>

      <label
        v-else-if="periodUnit === 'quarter'"
        class="sales-period-filter"
      >
        <span class="sr-only">
          조회 분기
        </span>

        <select v-model="selectedQuarter">
          <option
            v-for="quarter in 4"
            :key="quarter"
            :value="String(quarter)"
          >
            {{ quarter }}분기
          </option>
        </select>
      </label>

      <span
        v-else
        class="sales-period-description"
      >
        {{ selectedYear }}년 전체
      </span>

      <button
        type="button"
        class="sales-primary-button"
        :disabled="loading"
        @click="loadDashboard"
      >
        {{ loading ? "조회 중" : "조회" }}
      </button>
    </section>

    <p
      v-if="error"
      class="sales-error"
      role="alert"
    >
      {{ error }}
    </p>

    <section
      class="sales-kpi-grid"
      aria-label="매출 주요 지표"
    >
      <article
        v-for="card in kpiCards"
        :key="card.label"
        class="sales-kpi-card"
      >
        <small>
          {{ card.label }}
        </small>

        <strong>
          {{ card.value }}
        </strong>

        <p>
          {{ card.description }}
        </p>
      </article>
    </section>

    <section class="sales-chart-grid">
      <article class="sales-panel sales-trend-panel">
        <header class="sales-panel-header">
          <h2>강아지관 · 고양이관 매출 비교</h2>

          <p>
            {{ comparisonPeriod }} 기준 · 두 카테고리 매출 비교
          </p>
        </header>

        <div
          ref="salesChartElement"
          class="sales-trend-chart"
          role="img"
          :aria-label="`${comparisonPeriod} 카테고리별 매출 비교`"
        ></div>

        <p
          v-if="!comparisonCategoryHasData"
          class="sales-chart-note"
        >
          선택 기간의 카테고리별 매출 데이터가 없습니다.
        </p>

        <section class="sales-chart-footer" aria-label="강아지관과 고양이관 매출액">
          <h3>상품관별 매출</h3>
          <ul class="sales-chart-footer-list">
            <li
              v-for="category in categoryComparison.filter((item) => item.name !== '식기·급수관')"
              :key="category.name"
            >
              <span class="sales-chart-footer-name">
                <i :style="{ backgroundColor: category.color }" aria-hidden="true"></i>
                {{ category.name }}
              </span>
              <strong>{{ category.available ? formatWon(category.sales) : '데이터 없음' }}</strong>
            </li>
          </ul>
        </section>
      </article>

      <article class="sales-panel sales-category-panel">
        <header class="sales-panel-header">
          <h2>카테고리별 매출 비중</h2>

          <p>
            {{ comparisonPeriod }} 기준
          </p>
        </header>

        <div class="sales-donut-container">
          <div
            ref="categoryChartElement"
            class="sales-category-chart"
            role="img"
            aria-label="카테고리별 매출 비중"
          ></div>

          <section class="sales-donut-center">
            <small>
              카테고리 매출 합계
            </small>

            <strong>
              {{ (totalCategorySales / 10000).toLocaleString("ko-KR", { maximumFractionDigits: 1 }) }}만원
            </strong>
          </section>
        </div>

        <section class="sales-chart-footer" aria-label="전체 매출 및 식기·급수관 매출액">
          <h3>전체 매출 요약</h3>
          <ul class="sales-chart-footer-list">
            <li>
              <span class="sales-chart-footer-name">카테고리 매출 합계</span>
              <strong>{{ availableCategoryComparison.length ? formatWon(totalCategorySales) : '데이터 없음' }}</strong>
            </li>
            <li>
              <span class="sales-chart-footer-name">
                <i :style="{ backgroundColor: categoryDefinitions[2].color }" aria-hidden="true"></i>
                식기·급수관
              </span>
              <strong>
                {{ categoryComparison[2].available ? formatWon(categoryComparison[2].sales) : '데이터 없음' }}
              </strong>
            </li>
          </ul>
        </section>

        <p
          v-if="!availableCategoryComparison.length"
          class="sales-empty-message"
        >
          카테고리별 매출 데이터가 없습니다.
        </p>
      </article>

    </section>

    <section class="sales-panel sales-ai-panel">
      <header class="sales-ai-header">
        <section>
          <small>
            AI SALES FORECAST
          </small>

          <h2>다음 달 판매 예측</h2>

          <p>
            최근 28일 DB 주문을 바탕으로 한 추정치입니다.
            시드·테스트 주문이 포함되어 있을 수 있습니다.
          </p>
        </section>

        <button
          v-if="isAdmin"
          type="button"
          class="sales-primary-button"
          :disabled="retraining"
          @click="retrainModel"
        >
          {{ retraining ? "재학습 요청 중..." : "AI 모델 재학습" }}
        </button>
      </header>

      <section class="sales-ai-grid">
        <article class="sales-ai-result">
          <small>
            {{ forecast.targetMonth ? `${forecast.targetMonth} 예상 매출` : "다음 달 예상 매출" }}
          </small>

          <strong>
            {{ forecast.predictedSales == null ? "AI 연결 대기" : formatWon(forecast.predictedSales) }}
          </strong>

          <p v-if="forecast.growthRate != null">
            14일 일평균: {{ formatWon(forecast.previousDailyAverage) }} →
            {{ formatWon(forecast.recentDailyAverage) }} · 증감
            <b>
              {{ formatPercent(forecast.growthRate) }}
            </b>
          </p>
          <p v-if="forecast.forecastStartDate && forecast.forecastEndDate">
            예측 대상: {{ forecast.forecastStartDate }} ~ {{ forecast.forecastEndDate }}
            ({{ forecast.forecastDays }}일 · 누적값 아님)
          </p>
          <p v-if="forecast.forecastBaseDate">
            학습 기준일: {{ forecast.forecastBaseDate }} · 최근 28일 일별 매출 기준
          </p>
        </article>

        <article class="sales-ai-insight">
          <h3>판매 인사이트</h3>

          <p>
            {{ forecast.insight }}
          </p>
        </article>

        <article class="sales-ai-products">
          <h3>매출 상위 상품</h3>

          <ol v-if="forecast.topProducts.length">
            <li
              v-for="(product, index) in forecast.topProducts"
              :key="`${product.name}-${index}`"
            >
              <span>
                {{ product.name }}
              </span>

              <strong>
                {{ formatWon(product.sales) }}
              </strong>

              <small v-if="product.quantity != null">
                {{ formatNumber(product.quantity) }}개 판매
              </small>
            </li>
          </ol>

          <p
            v-else
            class="sales-empty-message"
          >
            상품별 데이터가 없습니다.
          </p>
        </article>
      </section>

      <p
        v-if="retrainMessage"
        class="sales-feedback"
        role="status"
      >
        {{ retrainMessage }}
      </p>
    </section>

    <section class="sales-panel sales-detail-panel">
      <header class="sales-panel-header">
        <h2>카테고리별 매출 분석</h2>

        <p>
          API에서 제공된 항목만 표시합니다.
        </p>
      </header>

      <div class="sales-table-scroll">
        <table class="sales-detail-table">
          <thead>
            <tr>
              <th scope="col">카테고리</th>
              <th scope="col">총 매출</th>
              <th scope="col">주문 건수</th>
              <th scope="col">평균 주문금액</th>
              <th scope="col">취소·환불</th>
              <th scope="col">전년 동기 대비</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="category in categories"
              :key="category.key"
            >
              <th scope="row">
                {{ category.name }}
              </th>

              <td>
                {{ formatWon(category.sales) }}
              </td>
              <td>{{ formatNumber(category.orders) }}건</td>
              <td>{{ category.orders ? formatWon(category.sales / category.orders) : '-' }}</td>
              <td>{{ formatWon(category.refund) }}</td>
              <td>{{ formatPercent(category.growth) }}</td>

            </tr>

            <tr v-if="!categories.length">
              <td
                colspan="6"
                class="sales-empty-table"
              >
                표시할 카테고리 데이터가 없습니다.
              </td>
            </tr>
          </tbody>

          <tfoot>
            <tr>
              <th scope="row">전체 합계</th>

              <td>
                {{ formatWon(summary.totalSales) }}
              </td>
              <td>{{ formatNumber(summary.totalOrders) }}건</td>
              <td>{{ formatWon(summary.averageOrderAmount) }}</td>
              <td>{{ formatWon(summary.refundAmount) }}</td>
              <td>-</td>

            </tr>
          </tfoot>
        </table>
      </div>
    </section>

    <section class="sales-panel sales-export-panel">
      <header class="sales-panel-header">
        <small>
          DATABASE EXPORT
        </small>

        <h2>분석 · 예측 데이터 다운로드</h2>

        <p>
          선택한 조회 기간의 서버 통계 데이터를 다운로드합니다.
        </p>
      </header>

      <section class="sales-export-controls">
        <label class="sales-export-section">
          포함 데이터

          <select v-model="exportSection">
            <option
              v-for="section in exportSections"
              :key="section.key"
              :value="section.key"
            >
              {{ section.label }}
            </option>
          </select>
        </label>

        <label>
          파일 형식

          <select v-model="exportFormat">
            <option
              v-for="format in exportFormats"
              :key="format.value"
              :value="format.value"
            >
              {{ format.label }}
            </option>
          </select>
        </label>

        <button
                    type="button"
          class="sales-primary-button"
          :disabled="exporting"
          @click="downloadStatistics"
        >
          {{ exporting ? "다운로드 중" : "DB 통계 다운로드" }}
        </button>
      </section>

      <p
        v-if="exportMessage"
        class="sales-feedback"
        role="status"
      >
        {{ exportMessage }}
      </p>
    </section>
  </main>
</template>







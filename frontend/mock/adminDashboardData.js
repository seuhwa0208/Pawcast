// Spring Boot 연동 전 관리자 대시보드가 실제 REST 응답 흐름으로 동작하도록 개발용 집계 데이터를 생성합니다.
const TREND_DATA = {
  month: {
    labels: [
      "1월",
      "2월",
      "3월",
      "4월",
      "5월",
      "6월",
      "7월",
      "8월",
      "9월",
      "10월",
      "11월",
      "12월",
    ],
    sales: [17, 19, 22, 25, 28, 30, 32, 36, 39, 47, 42, 45],
    orders: [420, 455, 520, 570, 610, 650, 710, 790, 840, 980, 910, 965],
  },
  quarter: {
    labels: ["1분기", "2분기", "3분기", "4분기"],
    sales: [58, 83, 107, 134],
    orders: [1395, 1830, 2340, 2855],
  },
  year: {
    labels: ["2023년", "2024년", "2025년", "2026년"],
    sales: [246, 309, 376, 429],
    orders: [5980, 7210, 8560, 9860],
  },
};

const CATEGORY_DATA = [
  {
    key: "DOG",
    name: "강아지",
    sales: 197960000,
    orders: 4510,
    refund: 6410000,
    growth: 13.9,
  },
  {
    key: "CAT",
    name: "고양이",
    sales: 144550000,
    orders: 3320,
    refund: 4380000,
    growth: 11.6,
  },
  {
    key: "ALL",
    name: "식기·급수",
    sales: 86050000,
    orders: 2030,
    refund: 2150000,
    growth: 11.2,
  },
];

export function createAdminDashboardResponse(query) {
  const unit = ["month", "quarter", "year"].includes(query.unit)
    ? query.unit
    : "month";
  const requestedCategories = String(query.categories || "DOG,CAT,ALL")
    .split(",")
    .filter(Boolean);
  const selectedYear = Number(query.year) || 2026;
  const yearFactor = { 2024: 0.78, 2025: 0.89, 2026: 1 }[selectedYear] || 1;
  const categories = CATEGORY_DATA.filter((row) =>
    requestedCategories.includes(row.key),
  ).map((row) => ({
    ...row,
    sales: Math.round(row.sales * yearFactor),
    orders: Math.round(row.orders * yearFactor),
    refund: Math.round(row.refund * yearFactor),
  }));
  const totalSales = categories.reduce((sum, row) => sum + row.sales, 0);
  const totalOrders = categories.reduce((sum, row) => sum + row.orders, 0);
  const totalRefund = categories.reduce((sum, row) => sum + row.refund, 0);
  const scale = categories.length / CATEGORY_DATA.length || 1;
  const trend = TREND_DATA[unit];
  let startIndex = 0;
  let endIndex = trend.labels.length;
  if (unit === "month") {
    startIndex = Math.max(0, Number(String(query.from).slice(5, 7)) - 1 || 0);
    endIndex = Math.min(12, Number(String(query.to).slice(5, 7)) || 12);
  } else if (unit === "quarter") {
    startIndex = Math.floor(
      Math.max(0, Number(String(query.from).slice(5, 7)) - 1 || 0) / 3,
    );
    endIndex = Math.ceil(
      Math.min(12, Number(String(query.to).slice(5, 7)) || 12) / 3,
    );
  }
  const trendLabels =
    unit === "year"
      ? Array.from({ length: 4 }, (_, index) => `${selectedYear - 3 + index}년`)
      : trend.labels.slice(startIndex, endIndex);

  return {
    success: true,
    filters: {
      unit,
      year: String(selectedYear),
      from: query.from || "2026-01-01",
      to: query.to || "2026-12-31",
      categories: requestedCategories,
    },
    summary: {
      totalSales,
      totalOrders,
      averageOrderAmount: totalOrders
        ? Math.round(totalSales / totalOrders)
        : 0,
      refundAmount: totalRefund,
      salesGrowthRate: 12.8,
      orderGrowthRate: 11.4,
      averageOrderGrowthRate: 1.3,
    },
    trend: {
      labels: trendLabels,
      sales: trend.sales
        .slice(startIndex, endIndex)
        .map((value) => Math.round(value * scale * yearFactor)),
      orders: trend.orders
        .slice(startIndex, endIndex)
        .map((value) => Math.round(value * scale * yearFactor)),
    },
    categories,
    forecast: {
      predictedSales: Math.round(totalSales * 1.084),
      growthRate: 8.4,
      analyzedAt: new Date().toISOString(),
      insight:
        "최근 판매 증가율과 계절 수요를 기준으로 다음 달에는 강아지 건식 사료와 고양이 영양제 판매가 증가할 가능성이 높습니다.",
      topProducts: [
        { name: "로얄캐닌 그레인프리 치킨", growthRate: 18.6 },
        { name: "ANF 오리 단일단백질", growthRate: 14.2 },
        { name: "뉴트리플랜 관절 영양제", growthRate: 11.8 },
      ],
    },
  };
}

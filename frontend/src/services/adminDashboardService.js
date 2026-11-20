// 관리자 매출 집계와 AI 예측 조회 조건을 REST 쿼리로 변환하는 서비스 계층입니다.
import { springRequest } from "@/services/http/springClient";

export const adminDashboardService = {
  getSalesDashboard(filters) {
    const periodMap = {
      day: "DAILY",
      month: "MONTHLY",
      quarter: "QUARTERLY",
      year: "YEARLY",
    };
    const date = new Date(filters.from || `${filters.year}-01-01`);
    const query = new URLSearchParams({
      period: periodMap[filters.unit] || "MONTHLY",
      year: filters.year,
    });
    if (filters.unit === "month") query.set("month", String(date.getMonth() + 1));
    if (filters.unit === "quarter") query.set("quarter", String(Math.floor(date.getMonth() / 3) + 1));
    if (filters.unit === "year") {
      query.set("startYear", filters.year);
      query.set("endYear", filters.year);
    }
    return springRequest(`/admin/sales?${query.toString()}`).then((rows) => {
      const periods = Array.isArray(rows) ? rows : [];
      const totalSales = periods.reduce((sum, row) => sum + Number(row.totalSales || 0), 0);
      const totalOrders = periods.reduce((sum, row) => sum + Number(row.orderCount || 0), 0);
      const refundAmount = periods.reduce((sum, row) => sum + Number(row.cancelAmount || 0), 0);
      return {
        summary: {
          totalSales,
          totalOrders,
          refundAmount,
          averageOrderAmount: totalOrders ? totalSales / totalOrders : 0,
        },
        trend: {
          labels: periods.map((row) => row.period),
          sales: periods.map((row) => Number(row.totalSales || 0)),
          orders: periods.map((row) => Number(row.orderCount || 0)),
        },
        // 카테고리·예측은 별도 API가 준비되기 전까지 빈 상태로 표시합니다.
        categories: [],
        forecast: { predictedSales: 0, growthRate: 0, insight: "예측 API 연동 대기 중입니다.", topProducts: [] },
      };
    });
  },
};

// 관리자 매출 집계와 AI 예측 조회 조건을 REST 쿼리로 변환하는 서비스 계층입니다.
import { springRequest } from "@/services/http/springClient";

export const adminDashboardService = {
  getSalesDashboard(filters) {
    const query = new URLSearchParams({ unit: filters.unit || "month", year: filters.year });
    if (filters.from) query.set("from", filters.from);
    if (filters.to) query.set("to", filters.to);
    return springRequest(`/admin/dashboard/sales?${query.toString()}`);
  },
  retrainSalesModel(forecastDate) {
    return springRequest(`/admin/dashboard/sales/retrain?forecastDate=${forecastDate}`, { method: "POST" });
  },
};

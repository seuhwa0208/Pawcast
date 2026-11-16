// 관리자 매출 집계와 AI 예측 조회 조건을 REST 쿼리로 변환하는 서비스 계층입니다.
import { springRequest } from "@/services/http/springClient";

export const adminDashboardService = {
  getSalesDashboard(filters) {
    const query = new URLSearchParams({
      unit: filters.unit,
      year: filters.year,
      from: filters.from,
      to: filters.to,
      categories: filters.categories.join(","),
    });
    return springRequest(`/admin/dashboard/sales?${query.toString()}`);
  },
};

// 관리자 화면은 Spring Boot를 단일 진입점으로 사용한다.
// AI 서버는 재학습·파일 생성 시 Spring 서버가 내부 키로 호출한다.
import { springRequest } from "@/services/http/springClient";

export const adminDashboardService = {
  getSalesYears: () => springRequest("/admin/dashboard/sales/years"),
  async getSalesDashboard(filters) {
    const query = new URLSearchParams({
      unit: filters.unit || "month",
      year: filters.year,
      from: filters.from,
      to: filters.to,
    });
    return springRequest(`/admin/dashboard/sales?${query.toString()}`);
  },
  async retrainSalesModel(forecastDate) {
    return springRequest(`/admin/ai/sales/retrain?forecastDate=${forecastDate}`, { method: "POST" });
  },
  async downloadStatistics({ startDate, endDate, format, section }) {
    const query = new URLSearchParams({
      // 브라우저는 Spring Controller의 camelCase 요청 파라미터에 맞춰 전송합니다.
      startDate,
      endDate,
      format,
      section,
    });
    const token = localStorage.getItem("powcast-access-token") || "";
    const baseUrl = (import.meta.env.VITE_SPRING_BASE_URL || "").replace(/\/$/, "");
    const response = await fetch(`${baseUrl}/api/admin/dashboard/sales/export?${query.toString()}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!response.ok) throw new Error("통계 파일을 생성하지 못했습니다.");
    const blob = await response.blob();
    const disposition = response.headers.get("content-disposition") || "";
    const filename = disposition.match(/filename="?([^";]+)"?/i)?.[1]
      || `pawcast-statistics.${format === "csv" ? "zip" : "xlsx"}`;
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  },
};

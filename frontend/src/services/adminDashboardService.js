// 매출 원본 집계는 Spring, AI 예측·재학습·파일 생성은 FastAPI에 직접 요청한다.
import { springRequest } from "@/services/http/springClient";
function dashboardQuery(values) {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value != null && value !== "") query.set(key, value);
  });
  return query.toString();
}

export const adminDashboardService = {
  getSalesYears: () => springRequest("/admin/dashboard/sales/years"),
  async getSalesDashboard(filters) {
    const query = dashboardQuery({
      unit: filters.unit || "month",
      year: filters.year,
      from: filters.from,
      to: filters.to,
    });
    return springRequest(`/admin/dashboard/sales?${query}`);
  },
  async retrainSalesModel(forecastDate) {
    return springRequest(
      `/admin/dashboard/sales/retrain?forecastDate=${encodeURIComponent(forecastDate)}`,
      { method: "POST" },
    );
  },
  async downloadStatistics({ startDate, endDate, format, section }) {
    const query = dashboardQuery({ startDate, endDate, format, section });
    const token = localStorage.getItem("powcast-access-token") || "";
    const response = await fetch(`/api/admin/dashboard/sales/export?${query}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      throw new Error(data.message || "통계 파일 다운로드에 실패했습니다.");
    }
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

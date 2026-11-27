// 주문 연도만 Spring에서 조회하고 매출 분석·예측·다운로드는 FastAPI에 직접 요청한다.
import { springRequest } from "@/services/http/springClient";
import { aiRequest } from "@/services/http/aiClient";

// FastAPI query parameter 이름(start_date/end_date)을 그대로 유지하면서
// 비어 있는 필터 값은 URL에 포함하지 않습니다.
function dashboardQuery(values) {
  const query = new URLSearchParams();

  Object.entries(values).forEach(([key, value]) => {
    if (value != null && value !== "") {
      query.set(key, String(value));
    }
  });

  return query.toString();
}

export const adminDashboardService = {
  getSalesYears: () => springRequest("/admin/dashboard/sales/years"),
  async getSalesDashboard(filters, forecastDate) {
    const query = dashboardQuery({
      unit: filters.unit || "month",
      start_date: filters.from,
      end_date: filters.to,
      forecast_date: forecastDate,
    });
    const { body } = await aiRequest(
      `/api/sales/dashboard?${query}`,
    );
    return body;
  },
  async retrainSalesModel(forecastDate) {
    const { body } = await aiRequest(
      `/api/sales/retrain?forecast_date=${encodeURIComponent(forecastDate)}`,
      { method: "POST" },
    );
    return body;
  },
  async downloadStatistics({ startDate, endDate, format, section }) {
    const query = dashboardQuery({
      start_date: startDate,
      end_date: endDate,
      format,
      section,
    });
    const { body: blob, response } = await aiRequest(`/api/sales/export?${query}`);
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

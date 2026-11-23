// 매출 원본 집계는 Spring, AI 예측·재학습·파일 생성은 FastAPI에 직접 요청한다.
import { springRequest } from "@/services/http/springClient";
import { aiRequest } from "@/services/http/aiClient";

function buildAiSalesQuery({ startDate, endDate, unit, format, section }) {
  const query = new URLSearchParams();

  if (startDate) query.set("start_date", startDate);
  if (endDate) query.set("end_date", endDate);
  if (unit) query.set("unit", unit);
  if (format) query.set("format", format);
  if (section) query.set("section", section);

  return query.toString();
}

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
  async getAiSalesDashboard({ from, to, unit }) {
    const query = buildAiSalesQuery({
      startDate: from,
      endDate: to,
      unit,
    });
    const { body } = await aiRequest(`/api/sales/dashboard?${query}`);
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
    const query = buildAiSalesQuery({ startDate, endDate, format, section });
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

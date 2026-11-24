import { springRequest } from "@/services/http/springClient";

// AI 카드도 Spring을 경유한다. 따라서 FastAPI와 브라우저의 JWT 검증값이 어긋나도
// 내부 API 키를 사용하는 서버 간 호출로 안전하게 조회된다.
export const aiAnalyticsService = {
  getReviewSentimentSummary: (limit = 5) =>
    springRequest(`/admin/dashboard/overview/review-sentiment?limit=${limit}`),
  getInventoryForecast: (limit = 5) =>
    springRequest(`/admin/dashboard/overview/inventory-forecast?limit=${limit}`),
};

import { aiRequest } from "@/services/http/aiClient";

// Spring에 없는 AI 분석 조회는 관리자 JWT를 붙여 FastAPI에 직접 요청한다.
export const aiAnalyticsService = {
  async getReviewSentimentSummary(limit = 5) {
    const { body } = await aiRequest(`/api/reviews/sentiment-summary?limit=${limit}`);
    return body;
  },
  async getInventoryForecast(limit = 5) {
    const { body } = await aiRequest(`/api/sales/inventory-forecast?limit=${limit}`);
    return body;
  },
};

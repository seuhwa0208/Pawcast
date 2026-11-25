import { aiRequest } from "@/services/http/aiClient";

export const aiAnalyticsService = {
  async getReviewSentimentSummary(limit = 5, days = 28) {
    const query = new URLSearchParams({
      limit: String(limit),
      days: String(days),
    });
    const { body } = await aiRequest(
      `/api/reviews/sentiment-summary?${query}`,
    );
    return body;
  },

  async getInventoryForecast(limit = 5) {
    const { body } = await aiRequest(`/api/sales/inventory-forecast?limit=${limit}`);
    return body;
  },

  async analyzePendingReviewSentiments(limit = 30) {
    const { body } = await aiRequest(
      `/api/reviews/analyze-pending?limit=${limit}`,
      { method: "POST" },
    );
    return body;
  },

  async analyzeRecentReviewSentiments(days = 28, limit = 100) {
    const query = new URLSearchParams({
      days: String(days),
      limit: String(limit),
    });
    const { body } = await aiRequest(
      `/api/reviews/analyze-recent?${query}`,
      { method: "POST" },
    );
    return body;
  },
};

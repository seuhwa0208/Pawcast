// FastAPI 추천 서버의 실제 요청·응답 계약만 호출합니다.
import { aiRequest } from "@/services/http/aiClient";

export const aiRecommendationService = {
  async recommend(payload) {
    const { body } = await aiRequest("/api/recommendations", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    return body;
  },
};

// FastAPI 추천 서버의 실제 요청·응답 계약만 호출합니다.
const AI_BASE_URL = (import.meta.env.VITE_AI_BASE_URL || "http://127.0.0.1:8000").replace(/\/$/, "");

export const aiRecommendationService = {
  async recommend(payload) {
    const response = await fetch(`${AI_BASE_URL}/api/recommendations`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.detail || "AI 추천을 불러오지 못했습니다.");
    return data;
  },
};

// 배송 완료 주문 항목을 구매 인증 근거로 사용해 상품 리뷰를 등록합니다.
import { springRequest } from "@/services/http/springClient";
import { aiRequest } from "@/services/http/aiClient";

// 구매 인증 리뷰는 주문 항목 ID를 기준으로 생성합니다.
export const reviewService = {
  // # 상품 상세의 실제 공개 리뷰 목록·평점 요약을 Spring API에서 조회한다.
  getProductReviews: (productId) => springRequest(`/products/${productId}/reviews`),
  getRankedProductReviews: async (productId) => {
    const { body } = await aiRequest(`/api/reviews/products/${productId}/ranked`);
    return Array.isArray(body) ? body : [];
  },
  create: async ({ images = [], ...payload }) => {
    const formData = new FormData();
    formData.append(
      "request",
      new Blob([JSON.stringify({ ...payload, images: [] })], {
        type: "application/json",
      }),
    );
    images.forEach((file) => formData.append("images", file, file.name));
    const review = await springRequest("/reviews", {
      method: "POST",
      body: formData,
    });
    try {
      const { body } = await aiRequest(`/api/reviews/${review.id}/analyze`, {
        method: "POST",
      });
      return { ...review, sentiment: body };
    } catch (error) {
      console.warn("리뷰는 저장됐지만 감정 분석 저장에 실패했습니다.", error);
      return { ...review, analysisPending: true };
    }
  },
};

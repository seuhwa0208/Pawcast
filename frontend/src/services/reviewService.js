
 // 배송 완료 주문 항목을 구매 인증 근거로 사용해 상품 리뷰를 등록합니다.
import { springRequest } from "@/services/http/springClient";
import { aiRequest } from "@/services/http/aiClient";

// 구매 인증 리뷰는 주문 항목 ID를 기준으로 생성합니다.
export const reviewService = {

  // # 상품 상세의 실제 공개 리뷰 목록·평점 요약을 Spring API에서 조회한다.
  getProductReviews: (productId) =>
    springRequest(`/products/${productId}/reviews`),

  // AI 분석 기반 상품 리뷰 순위 조회
  getRankedProductReviews: async (productId) => {
    const { body } = await aiRequest(
      `/api/reviews/products/${productId}/ranked`
    );

    return Array.isArray(body) ? body : [];
  },

  // 리뷰 단건 조회
  getReview: (reviewId) =>
    springRequest(`/reviews/${reviewId}`),

  // 리뷰 등록
  create: async ({ images = [], ...payload }) => {
    const formData = new FormData();

    formData.append(
      "request",
      new Blob(
        [JSON.stringify({ ...payload, images: [] })],
        {
          type: "application/json",
        }
      ),
    );

    images.forEach((file) =>
      formData.append("images", file, file.name)
    );

    const review = await springRequest("/reviews", {
      method: "POST",
      body: formData,
    });

    try {
      const { body } = await aiRequest(
        `/api/reviews/${review.id}/analyze`,
        {
          method: "POST",
        }
      );

      return {
        ...review,
        sentiment: body,
      };

    } catch (error) {
      console.warn(
        "리뷰는 저장됐지만 감정 분석 저장에 실패했습니다.",
        error
      );

      return {
        ...review,
        analysisPending: true,
      };
    }
  },

  // 회원 본인의 리뷰 수정
  update: async (
    reviewId,
    {
      rating,
      content,
      images = null,
      contentChanged = false,
    }
  ) => {

    const formData = new FormData();

    // null: 기존 이미지 유지
    // 빈 배열: 기존 이미지 전체 삭제
    // 파일 배열: 기존 이미지 전체 교체
    const request = {
      rating,
      content,
      images: images === null ? null : [],
    };

    formData.append(
      "request",
      new Blob(
        [JSON.stringify(request)],
        {
          type: "application/json",
        }
      ),
    );

    // 새로운 이미지가 있는 경우에만 파일을 전송합니다.
    if (Array.isArray(images)) {
      images.forEach((file) => {
        formData.append("images", file, file.name);
      });
    }

    // Spring Boot 리뷰 수정 API 호출
    const review = await springRequest(
      `/reviews/${reviewId}`,
      {
        method: "PATCH",
        body: formData,
      }
    );

    // 리뷰 본문이 변경되지 않았다면 기존 감성 분석 결과를 유지합니다.
    if (!contentChanged) {
      return review;
    }

    // 리뷰 본문이 변경된 경우 AI 감성 분석을 다시 실행합니다.
    try {
      const { body } = await aiRequest(
        `/api/reviews/${review.id}/analyze`,
        {
          method: "POST",
        }
      );

      return {
        ...review,
        sentiment: body,
      };

    } catch (error) {
      console.warn(
        "리뷰는 수정됐지만 감정 재분석에 실패했습니다.",
        error
      );

      return {
        ...review,
        analysisPending: true,
      };
    }
  },
};

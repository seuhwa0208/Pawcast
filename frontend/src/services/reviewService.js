// 배송 완료 주문 항목을 구매 인증 근거로 사용해 상품 리뷰를 등록합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

// 구매 인증 리뷰는 주문 항목 ID를 기준으로 생성합니다.
export const reviewService = {
  create: (payload) =>
    springRequest("/reviews", {
      method: "POST",
      body: jsonBody(payload),
    }),
};

// 회원 주문 목록과 주문 상세 정보를 Spring 주문 API에서 조회합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const orderService = {
  // 현재 Spring 주문 목록 API는 별도 기간·상태 쿼리를 받지 않고 로그인 회원 전체 목록을 반환합니다.
  getMyOrders: () => springRequest("/orders"),
  getOrder: (orderId) => springRequest(`/orders/${orderId}`),
  cancelOrder: (orderId, cancelReason) =>
    springRequest(`/orders/${orderId}/cancel`, {
      method: "POST",
      body: jsonBody({ cancelReason }),
    }),
  getRefund: (orderId) => springRequest(`/orders/${orderId}/refunds`),
  requestRefund: (orderId, cancelReason) =>
    springRequest(`/orders/${orderId}/refunds`, {
      method: "POST",
      body: jsonBody({ cancelReason }),
    }),
};

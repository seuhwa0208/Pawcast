// 회원 주문 목록과 주문 상세 정보를 Spring 주문 API에서 조회합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const orderService = {
  // 현재 Spring 주문 목록 API는 별도 기간·상태 쿼리를 받지 않고 로그인 회원 전체 목록을 반환합니다.
  getMyOrders: () => springRequest("/orders"),
  getOrder: (orderId) => springRequest(`/orders/${orderId}`),
  getGuestOrder: ({ email, orderNumber }) =>
    springRequest(
      `/orders/guest?email=${encodeURIComponent(email)}&orderNumber=${encodeURIComponent(orderNumber)}`,
    ),
  cancelOrder: (orderId, cancelReason) =>
    springRequest(`/orders/${orderId}/cancel`, {
      method: "POST",
      body: jsonBody({ cancelReason }),
    }),
  // 환불 미신청 주문의 404는 취소·반품 화면에서 "신청 가능" 상태로 표시한다.
  getRefund: (orderId) =>
    springRequest(`/orders/${orderId}/refunds`, {
      suppressGlobalErrorStatuses: [404],
    }),
  requestRefund: (orderId, cancelReason) =>
    springRequest(`/orders/${orderId}/refunds`, {
      method: "POST",
      body: jsonBody({ cancelReason }),
    }),
};

// 회원 주문 목록과 주문 상세 정보를 Spring 주문 API에서 조회합니다.
import { springRequest } from "@/services/http/springClient";

export const orderService = {
  getMyOrders: ({ period = "3m", status, from, to } = {}) => {
    const query = new URLSearchParams({ period });
    if (status) query.set("status", status);
    if (from) query.set("from", from);
    if (to) query.set("to", to);
    return springRequest(`/orders?${query.toString()}`);
  },
  getOrder: (orderId) => springRequest(`/orders/${orderId}`),
  cancelOrder: (orderId) =>
    springRequest(`/orders/${orderId}/cancel`, { method: "POST" }),
  getRefund: (orderId) => springRequest(`/orders/${orderId}/refunds`),
};

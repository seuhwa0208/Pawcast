// 회원 주문 목록과 주문 상세 정보를 Spring 주문 API에서 조회합니다.
import { springRequest } from "@/services/http/springClient";

export const orderService = {
  getMyOrders: ({ period = "6m", status } = {}) => {
    const query = new URLSearchParams({ period });
    if (status) query.set("status", status);
    return springRequest(`/orders?${query.toString()}`);
  },
  getOrder: (orderId) => springRequest(`/orders/${orderId}`),
};

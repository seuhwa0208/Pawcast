// 주문 생성부터 PG 결제 준비·승인까지 PAWCAST 백엔드 결제 흐름을 캡슐화합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const paymentService = {
  createOrder: (payload) =>
    springRequest("/orders", {
      method: "POST",
      body: jsonBody(payload),
    }),
  preparePayment: (orderId, payload, orderAccessToken) =>
    springRequest(`/orders/${orderId}/payments`, {
      method: "POST",
      body: jsonBody(payload),
      headers: orderAccessToken
        ? { Authorization: `Bearer ${orderAccessToken}` }
        : {},
    }),
  confirmPayment: (orderId, paymentId, payload, orderAccessToken) =>
    springRequest(`/orders/${orderId}/payments/${paymentId}/confirm`, {
      method: "POST",
      body: jsonBody(payload),
      headers: orderAccessToken
        ? { Authorization: `Bearer ${orderAccessToken}` }
        : {},
    }),
};

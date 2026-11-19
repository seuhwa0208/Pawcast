// 회원 장바구니 조회·추가·수량 변경·삭제와 비회원 장바구니 병합 API를 제공합니다.
import { jsonBody, springRequest } from "@/services/http/springClient";

export const cartService = {
  getCart: () => springRequest("/cart"),
  addItem: (payload) =>
    springRequest("/cart/items", { method: "POST", body: jsonBody(payload) }),
  updateItem: (itemId, quantity) =>
    springRequest(`/cart/items/${itemId}`, {
      method: "PATCH",
      body: jsonBody({ quantity }),
    }),
  removeItem: (itemId) =>
    springRequest(`/cart/items/${itemId}`, { method: "DELETE" }),
  mergeGuestCart: (items) =>
    springRequest("/cart/merge", {
      method: "POST",
      body: jsonBody({ items }),
    }),
};

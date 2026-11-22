// Spring CartController가 제공하는 회원 장바구니 조회·추가·수량 변경·삭제 API입니다.
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
};

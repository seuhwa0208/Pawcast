import { springRequest } from "@/services/http/springClient";

export const wishlistService = {
  list: () => springRequest("/members/me/wishlist"),
  add: (productId) =>
    springRequest(`/members/me/wishlist/${productId}`, { method: "PUT" }),
  remove: (productId) =>
    springRequest(`/members/me/wishlist/${productId}`, { method: "DELETE" }),
};

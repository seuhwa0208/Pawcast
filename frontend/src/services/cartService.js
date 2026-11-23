
import { jsonBody, springRequest } from "@/services/http/springClient";

export const cartService = {
    getCart: () =>
        springRequest("/cart"),
    addItem: (payload) =>
        springRequest("/cart/items", {
            method: "POST",
            body: jsonBody(payload),
        }),
    updateItem: (itemId, quantity) =>
        springRequest(`/cart/items/${itemId}`, {
            method: "PATCH",
            body: jsonBody({ quantity }),
        }),
    removeItem: (itemId) =>
        springRequest(`/cart/items/${itemId}`, {
            method: "DELETE",
        }),
    mergeGuestCart: (mergeId, items) =>
        springRequest("/cart/merge", {
            method: "POST",
            body: jsonBody({
                mergeId,
                items: items.map((item) => ({
                    variantId: Number(item.variantId),
                    quantity: Number(item.quantity),
                })),
            }),
        }),
};

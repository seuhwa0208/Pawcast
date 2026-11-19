package com.project.pawcast.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items,
        long totalQuantity,
        BigDecimal totalAmount
) {
    public static CartResponse of(Long cartId, List<CartItemResponse> items) {
        return new CartResponse(cartId, List.copyOf(items),
                items.stream().mapToLong(CartItemResponse::quantity).sum(),
                items.stream().map(CartItemResponse::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}

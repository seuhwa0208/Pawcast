package com.project.pawcast.order.dto;

public record OrderItemRequest(
        Long productVariantId,
        Integer quantity
) {
}

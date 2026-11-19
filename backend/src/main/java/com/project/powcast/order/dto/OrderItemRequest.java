package com.project.powcast.order.dto;

public record OrderItemRequest(
        Long productVariantId,
        Integer quantity
) {
}

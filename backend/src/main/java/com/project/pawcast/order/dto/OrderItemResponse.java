package com.project.pawcast.order.dto;

import com.project.pawcast.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long orderItemId,
        Long orderId,
        Long productVariantId,
        String productName,
        int quantity,
        String optionLabel,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getOrder().getId(),
                item.getProductVariant().getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getOptionLabel(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}
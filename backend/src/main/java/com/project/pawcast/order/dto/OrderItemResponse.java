package com.project.pawcast.order.dto;

import com.project.pawcast.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long orderItemId,
        Long orderId,
        Long productId,
        Long productVariantId,
        String productName,
        String imageUrl,
        int quantity,
        String optionLabel,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    // OrderItem 엔티티를 응답 DTO로 변환
    public static OrderItemResponse from(OrderItem item) {
        return from(item, null);
    }

    public static OrderItemResponse from(OrderItem item, String imageUrl) {
        return new OrderItemResponse(
                item.getId(),
                item.getOrder().getId(),
                item.getProductVariant().getProduct().getId(),
                item.getProductVariant().getId(),
                item.getProductName(),
                imageUrl,
                item.getQuantity(),
                item.getOptionLabel(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}

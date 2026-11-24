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
        BigDecimal totalPrice,
        Long reviewId
) {

    // OrderItem 엔티티를 응답 DTO로 변환
    public static OrderItemResponse from(OrderItem item) {
        return from(item, null);
    }

    // 기존 주문 항목 및 대표 이미지 응답 변환
    public static OrderItemResponse from(
            OrderItem item,
            String imageUrl
    ) {
        return from(item, imageUrl, null);
    }

    // 주문 항목, 대표 이미지, 작성한 리뷰 ID를 응답 DTO로 변환
    public static OrderItemResponse from(
            OrderItem item,
            String imageUrl,
            Long reviewId
    ) {
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
                item.getTotalPrice(),
                reviewId
        );
    }
}

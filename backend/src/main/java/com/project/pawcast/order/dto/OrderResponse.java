package com.project.pawcast.order.dto;

import com.project.pawcast.member.entity.Members;
import com.project.pawcast.order.entity.Order;
import com.project.pawcast.order.entity.OrderItem;
import com.project.pawcast.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record OrderResponse(
        Long orderId,
        Members member,
        String orderNumber,
        OrderStatus status,
        BigDecimal total_amount,
        BigDecimal discount_amount,
        BigDecimal shipping_fee,
        BigDecimal final_amount,
        String recipient_name,
        String recipient_phone,
        String recipient_email,
        String delivery_zipcode,
        String delivery_address1,
        String delivery_address2,
        String delivery_message,
        LocalDateTime created_at,
        List<OrderItemResponse> items
) {

    // Order 엔티티와 주문 항목 목록을 응답 DTO로 변환
    public static OrderResponse from(
            Order order,
            List<OrderItem> orderItems
    ) {
        return from(
                order,
                orderItems,
                Map.of()
        );
    }

    // 기존 주문 항목 및 상품 대표 이미지 응답 변환
    public static OrderResponse from(
            Order order,
            List<OrderItem> orderItems,
            Map<Long, String> mainImagesByProductId
    ) {
        return from(
                order,
                orderItems,
                mainImagesByProductId,
                Map.of()
        );
    }

    // 주문 항목별 리뷰 ID를 포함하여 주문 응답 DTO로 변환
    public static OrderResponse from(
            Order order,
            List<OrderItem> orderItems,
            Map<Long, String> mainImagesByProductId,
            Map<Long, Long> reviewIdsByOrderItemId
    ) {
        return new OrderResponse(
                order.getId(),
                order.getMember(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getFinalAmount(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getRecipientEmail(),
                order.getDeliveryZipcode(),
                order.getDeliveryAddress1(),
                order.getDeliveryAddress2(),
                order.getDeliveryMessage(),
                order.getCreatedAt(),
                orderItems.stream()
                        .map(item -> OrderItemResponse.from(
                                item,
                                mainImagesByProductId.get(
                                        item.getProductVariant()
                                                .getProduct()
                                                .getId()
                                ),
                                reviewIdsByOrderItemId.get(
                                        item.getId()
                                )
                        ))
                        .toList()
        );
    }
}

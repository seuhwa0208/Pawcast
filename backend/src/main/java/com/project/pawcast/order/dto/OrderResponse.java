package com.project.pawcast.order.dto;

import com.project.pawcast.order.entity.Order;
import com.project.pawcast.order.entity.OrderItem;
import com.project.pawcast.order.entity.OrderStatus;
import com.project.pawcast.pets.dto.PetsResponse;
import com.project.pawcast.pets.entity.Pets;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long memberId,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal finalAmount,
        String recipientName,
        String recipientPhone,
        String recipientEmail,
        String deliveryZipcode,
        String deliveryAddress1,
        String deliveryAddress2,
        String deliveryMessage,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(),
                order.getMember() == null ? null : order.getMember().getId(),
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
                items.stream().map(OrderItemResponse::from).toList()
        );
    }

    public record OrderItemResponse(
            Long id, Long productId, Long variantId, String productName,
            String optionLabel, Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(), item.getProductVariant().getProduct().getId(),
                    item.getProductVariant().getId(), item.getProductName(),
                    item.getOptionLabel(), item.getQuantity(), item.getUnitPrice(), item.getTotalPrice()
            );
        }
    }
}

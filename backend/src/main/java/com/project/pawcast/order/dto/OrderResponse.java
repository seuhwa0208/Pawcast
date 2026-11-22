package com.project.pawcast.order.dto;

import com.project.pawcast.member.entity.Members;
import com.project.pawcast.order.entity.Order;
import com.project.pawcast.order.entity.OrderItem;
import com.project.pawcast.order.entity.OrderStatus;
import com.project.pawcast.pets.dto.PetsResponse;
import com.project.pawcast.pets.entity.Pets;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    public static OrderResponse from(Order order, List<OrderItem> orderItems) {
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
                orderItems.stream().map(OrderItemResponse::from).toList()
        );
    }
}

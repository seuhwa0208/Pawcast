package com.project.pawcast.order.dto;

import com.project.pawcast.order.entity.OrderStatus;

public record OrderStatusRequest(
        OrderStatus orderStatus
) {
}

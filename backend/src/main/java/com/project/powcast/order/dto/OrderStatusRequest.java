package com.project.powcast.order.dto;

import com.project.powcast.order.entity.OrderStatus;

public record OrderStatusRequest(
        OrderStatus orderStatus
) {
}

package com.project.pawcast.refund.dto;

import com.project.pawcast.refund.entity.Refund;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse (
        Long refundId,
        Long paymentId,
        Long orderId,
        String orderNumber,
        String providerRefundKey,
        BigDecimal refundAmount,
        String reason,
        Refund.RefundStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getPayment().getOrder().getId(),
                refund.getPayment().getOrder().getOrderNumber(),
                refund.getProviderRefundKey(),
                refund.getRefundAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getCreatedAt(),
                refund.getCompletedAt()
        );
    }
}

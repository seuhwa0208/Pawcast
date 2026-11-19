package com.project.pawcast.refund.dto;

import com.project.pawcast.refund.entity.Refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse (
        Long refundId,
        Long paymentId,
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
                refund.getProviderRefundKey(),
                refund.getRefundAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getCreatedAt(),
                refund.getCompletedAt()
        );
    }
}

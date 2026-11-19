package com.project.pawcast.payment.dto;

import com.project.pawcast.payment.entity.Payment;
import com.project.pawcast.payment.entity.PaymentsStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDtos {

    public record PaymentResponse(
            Long paymentId,
            String orderNumber,
            String paymentKey,
            BigDecimal amount,
            Payment.PaymentMethod paymentMethod,
            PaymentsStatus status,
            LocalDateTime paidAt
    ) {
        public static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                    payment.getId(),
                    payment.getOrder().getOrderNumber(),
                    payment.getPaymentKey(),
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    payment.getStatus(),
                    payment.getPaidAt()
            );
        }

    }

    public record PaymentRequest(
            String paymentKey,
            String orderNumber,
            Long amount
    ) {}
}

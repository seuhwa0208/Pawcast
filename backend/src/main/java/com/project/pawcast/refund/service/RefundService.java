package com.project.pawcast.refund.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.order.entity.Order;
import com.project.pawcast.order.repository.OrderRepository;
import com.project.pawcast.payment.entity.Payment;
import com.project.pawcast.payment.repository.PaymentRepository;
import com.project.pawcast.refund.dto.RefundResponse;
import com.project.pawcast.refund.entity.Refund;
import com.project.pawcast.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    // 추가
    @Transactional
    public Refund createRefund(Payment payment, String cancelReason, String providerRefundKey){
        Refund refund = Refund.builder()
                .payment(payment)
                .providerRefundKey(providerRefundKey)
                .idempotencyKey(UUID.randomUUID().toString())
                .refundAmount(payment.getAmount())
                .reason(cancelReason)
                .status(Refund.RefundStatus.COMPLETED)
                .build();

        return refundRepository.save(refund);
    }

    // 조회
    public RefundResponse detailRefund(Long orderId, Long memberId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        if (order.getMember() == null || !order.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.NOT_ORDER_OWNER);}
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));
        Refund refund = refundRepository.findByPaymentId(payment.getId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_REFUND));

        return RefundResponse.from(refund);
    }
}

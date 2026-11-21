package com.project.pawcast.refund.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.order.entity.Order;
import com.project.pawcast.order.entity.OrderItem;
import com.project.pawcast.order.entity.OrderStatus;
import com.project.pawcast.order.repository.OrderItemRepository;
import com.project.pawcast.order.repository.OrderRepository;
import com.project.pawcast.payment.entity.Payment;
import com.project.pawcast.payment.entity.PaymentsStatus;
import com.project.pawcast.payment.repository.PaymentRepository;
import com.project.pawcast.payment.service.PaymentService;
import com.project.pawcast.product.repository.ProductRepository;
import com.project.pawcast.refund.dto.RefundResponse;
import com.project.pawcast.refund.entity.Refund;
import com.project.pawcast.refund.repository.RefundRepository;
import com.project.pawcast.security.AuthorizationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final AuthorizationValidator validator;
    private final MembersRepository membersRepository;

    // 환불 정보 생성
    @Transactional
    public RefundResponse createRefund( Payment payment, String cancelReason, String providerRefundKey, Refund.RefundStatus status, LocalDateTime completedAt) {
        Refund refund = Refund.builder()
                .payment(payment)
                .providerRefundKey(providerRefundKey)
                .idempotencyKey(UUID.randomUUID().toString())
                .refundAmount(payment.getAmount())
                .reason(cancelReason)
                .status(status)
                .completedAt(completedAt)
                .build();

        refundRepository.save(refund);
        return RefundResponse.from(refund);
    }

    // 환불 확정
    @Transactional
    public void completeRefund(Refund refund, String providerRefundKey, LocalDateTime completedAt) {
        refund.setProviderRefundKey(providerRefundKey);
        refund.setStatus(Refund.RefundStatus.COMPLETED);
        refund.setCompletedAt(completedAt);
    }

    // 조회
    public RefundResponse detailRefund(Long orderId, Long memberId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        if (order.getMember() == null || !order.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.NOT_ORDER_OWNER);}
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));
        Refund refund = refundRepository.findByPaymentId(payment.getId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_REFUND));

        return RefundResponse.from(refund);
    }

    // 환불 기능 - 환불 요청 기능에 가깝다
    // 배송 완료 상태 -> 환불하기 -> 환불 정보가 생성(이유 적혀있음) -> 관리자가 결제 취소 누르면 -> 결제 취소
    @Transactional
    public void refundPayment(Long orderId, Long memberId, String cancelReason) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));
        // 회원 검사
        if (order.getMember() == null || !order.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.NOT_ORDER_OWNER);}
        // 배송 완료 상태 인지 검사
        if (order.getStatus() != OrderStatus.DELIVERED) {throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);}
        // 결제가 완료되었는지 검사
        if (payment.getStatus() != PaymentsStatus.PAID) {throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);}
        // 기존 환불 요청 확인 - 중복검사
        if (refundRepository.existsByPaymentId(payment.getId())) {throw new BusinessException(ErrorCode.ALREADY_EXISTS_REFUND);}
        // 환불 정보 생성 - 대기중, 완료 날짜 없음
        createRefund(payment, cancelReason, null, Refund.RefundStatus.PENDING, null);

        // 이 이후로는 관리자가 승인하면 - 취소기능
    }

    //환불 승인
    @Transactional
    public void approveRefund(Long refundId, Long memberId) {
        //권한 검사
        Members member = membersRepository.findById(memberId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);
        
        // 환불 정보 조회 및 잠금
        Refund refund = refundRepository.findByIdForUpdate(refundId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_REFUND));
        // 환불 대기중인가?
        if (refund.getStatus() != Refund.RefundStatus.PENDING) {throw new BusinessException(ErrorCode.INVALID_REFUND_STATUS);}

        Order order = orderRepository.findByIdForUpdate(refund.getPayment().getOrder().getId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));

        // 배송 완료된 상품인가?
        if (order.getStatus() != OrderStatus.DELIVERED) {throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);}
        // 결제가 끝난 건인가?
        if (refund.getPayment().getStatus() != PaymentsStatus.PAID) {throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);}

        // 토스 결제 취소
        String providerRefundKey = paymentService.cancelPayment(order.getId(),refund.getReason());

        // 결제 상태 변경
        refund.getPayment().setStatus(PaymentsStatus.CANCELLED);
        // 주문 상태 변경
        order.setStatus(OrderStatus.CANCELLED);
        // 환불 완료 처리
        completeRefund(refund, providerRefundKey, LocalDateTime.now());

        // 상품 판매량 감소
        List<OrderItem> items = orderItemRepository.findAllByOrder_Id(order.getId());
        for (OrderItem item : items) {
            productRepository.decreaseSalesCount(item.getProductVariant().getProduct().getId(), item.getQuantity());
        }
    }
}

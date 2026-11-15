package com.project.powcast.payment.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.order.entity.Order;
import com.project.powcast.order.entity.OrderItem;
import com.project.powcast.order.entity.OrderStatus;
import com.project.powcast.order.repository.OrderItemRepository;
import com.project.powcast.order.repository.OrderRepository;
import com.project.powcast.payment.client.TossPaymentClient;
import com.project.powcast.payment.dto.PaymentDtos;
import com.project.powcast.payment.entity.Payment;
import com.project.powcast.payment.entity.PaymentsStatus;
import com.project.powcast.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final InventoryService inventoryService;
    private final OrderItemRepository orderItemRepository;

    // 결제
    @Transactional
    public PaymentDtos.PaymentResponse approve(Long orderId, Long memberId, String paymentKey, String tossOrderId, Long amount) {

        // 1. 주문 조회
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        // 2. 본인 주문인지 확인
        if (order.getMember() == null || !order.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.NOT_ORDER_OWNER);}
        // 3. 결제 대기 상태인지 확인
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);}
        // 4. 주문번호 검증
        if (!order.getOrderNumber().equals(tossOrderId)) {throw new BusinessException(ErrorCode.INVALID_ORDER_NUMBER);}
        // 5. 결제 금액 검증
        BigDecimal expectedAmount = order.getFinalAmount();
        if (expectedAmount.compareTo(BigDecimal.valueOf(amount)) != 0) {throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);}
        // 6. 중복 결제 확인
        if (paymentRepository.findByOrderId(orderId).isPresent()) {throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);}
        // 7. Toss 결제 승인 요청
        Map<String, Object> tossResponse = tossPaymentClient.approve(paymentKey, tossOrderId, amount);
        // 8. 결제 수단 확인
        String method = (String) tossResponse.get("method");
        Payment.PaymentMethod paymentMethod = switch (method) {
            case "카드" -> Payment.PaymentMethod.CARD;
            case "계좌이체" -> Payment.PaymentMethod.BANK;
            case "가상계좌" -> Payment.PaymentMethod.VIRTUAL_ACCOUNT;
            case "간편결제" -> Payment.PaymentMethod.EASY_PAY;
            default -> throw new BusinessException(ErrorCode.INVALID_PAYMENT_METHOD);
        };

        // 9. 결제 승인 시간
        LocalDateTime paidAt = OffsetDateTime.parse((String) tossResponse.get("approvedAt")).toLocalDateTime();
        // 10. Payment 생성
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .provider("TOSS")
                .paymentKey(paymentKey)
                .idempotencyKey("ORDER-" + order.getId())
                .amount(expectedAmount)
                .status(PaymentsStatus.PAID)
                .paidAt(paidAt)
                .build();

        paymentRepository.save(payment);

        // 11. 주문 상태 변경
        order.setStatus(OrderStatus.PAID);

        // 재고 차감
        // order -> orderItem -> variantId
        List<OrderItem> items = orderItemRepository.findAllByOrder_Id(order.getId());
        for (OrderItem item : items){
            inventoryService.decreaseStock(item.getProductVariant().getId(), item.getQuantity());
        }

        // 12. 응답 반환
        return new PaymentDtos.PaymentResponse(
                payment.getId(),
                order.getOrderNumber(),
                payment.getPaymentKey(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }//결제
    
    //결제 정보 조회
    public PaymentDtos.PaymentResponse detailPayment(Long orderId, Long memberId){
        // 주문 정보
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        // 주문 정보 바탕으로 회원 검사
        if (order.getMember() == null || !order.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.NOT_ORDER_OWNER);}
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));
        return PaymentDtos.PaymentResponse.from(payment);
    }
    
    //결제 취소
    @Transactional
    public void cancelPayment(Long orderId, Long memberId, String cancelReason){
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        if (order.getMember() == null || !order.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.NOT_ORDER_OWNER);}
        //결제 검사
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));
        //결제 상태 검사
        if (payment.getStatus() != PaymentsStatus.PAID) {throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);}
        // 토스 취소
        tossPaymentClient.cancel(payment.getPaymentKey(), cancelReason);
        // 결제 상태 취소
        payment.setStatus(PaymentsStatus.CANCELLED);
        order.setStatus(OrderStatus.CANCELLED);
        // 재고 복구
        List<OrderItem> items = orderItemRepository.findAllByOrder_Id(order.getId());
        for (OrderItem item : items){
            inventoryService.increaseStock(item.getProductVariant().getId(), item.getQuantity());
        }
    }
}

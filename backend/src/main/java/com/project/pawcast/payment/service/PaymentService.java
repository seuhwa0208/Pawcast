package com.project.pawcast.payment.service;

import com.project.pawcast.cart.service.CartService;
import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.order.entity.Order;
import com.project.pawcast.order.entity.OrderItem;
import com.project.pawcast.order.entity.OrderStatus;
import com.project.pawcast.order.repository.OrderItemRepository;
import com.project.pawcast.order.repository.OrderRepository;
import com.project.pawcast.payment.client.TossPaymentClient;
import com.project.pawcast.payment.dto.PaymentDtos;
import com.project.pawcast.payment.dto.TossCancelResponse;
import com.project.pawcast.payment.entity.Payment;
import com.project.pawcast.payment.entity.PaymentsStatus;
import com.project.pawcast.payment.repository.PaymentRepository;
import com.project.pawcast.product.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
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
    private final CartService cartService;

    // 결제
    @Transactional
    public PaymentDtos.PaymentResponse approve(Long orderId, Long memberId, String paymentKey, String orderNumber, Long amount) {

        if (amount == null || amount < 0 || paymentKey == null || paymentKey.isBlank() || orderNumber == null || orderNumber.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 동일 주문의 중복 결제·장바구니 정리를 직렬화합니다.
        // 1. 주문 조회
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        // 2. 본인 주문인지 확인
        if (order.getMember() == null || !order.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.NOT_ORDER_OWNER);}
        // 3. 결제 대기 상태인지 확인
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);}
        // 4. 주문번호 검증
        if (!order.getOrderNumber().equals(orderNumber)) {throw new BusinessException(ErrorCode.INVALID_ORDER_NUMBER);}
        // 5. 결제 금액 검증
        BigDecimal expectedAmount = order.getFinalAmount();
        if (expectedAmount.compareTo(BigDecimal.valueOf(amount)) != 0) {throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);}
        // 6. 중복 결제 확인
        if (paymentRepository.findByOrderId(orderId).isPresent()) {throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);}
        // 7. Toss 결제 승인 요청
        Map<String, Object> tossResponse = tossPaymentClient.approve(paymentKey, orderNumber, amount);
        // 입금 대기·실패 상태는 구매 완료로 처리하지 않습니다.
        if (tossResponse == null || !"DONE".equals(tossResponse.get("status"))) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

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
        // 여러 옵션의 재고를 동시에 갱신할 때 항상 variant ID 순서로 잠그면
        // 서로 다른 결제 요청 사이의 교착 상태 가능성을 줄일 수 있습니다.
        List<OrderItem> items = orderItemRepository.findAllByOrder_Id(order.getId())
                .stream()
                .sorted(Comparator.comparing(item -> item.getProductVariant().getId()))
                .toList();
        for (OrderItem item : items){
            inventoryService.decreaseStock(item.getProductVariant().getId(), item.getQuantity());
        }

        // 결제 저장과 같은 트랜잭션에서 구매한 옵션만 장바구니에서 삭제합니다.
        // 주문 생성·결제 실패·결제 취소 경로에서는 호출하지 않습니다.
        cartService.removePurchasedItems(memberId, items);

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

    //공통 기능 - 결제 취소
    @Transactional
    public String cancelPayment(Long orderId,String cancelReason){
        //결제 검사
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));
        //결제 상태 검사
        if (payment.getStatus() != PaymentsStatus.PAID) {throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);}
        // 토스 취소
        TossCancelResponse response = tossPaymentClient.cancel(payment.getPaymentKey(), cancelReason);
        TossCancelResponse.CancelInfo cancelInfo = response.cancels().get(response.cancels().size() - 1);
        // 취소 성공 확인
        // Toss 취소 상태 - DONE, CANCELED
        if (!"DONE".equals(cancelInfo.cancelStatus())) {throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);}

        // 결제 상태 취소
        payment.setStatus(PaymentsStatus.CANCELLED);
        return cancelInfo.transactionKey();
    }
    



}

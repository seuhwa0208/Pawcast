package com.project.pawcast.order.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.common.util.OrderNumberGenerator;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.order.dto.OrderItemRequest;
import com.project.pawcast.order.dto.OrderRequest;
import com.project.pawcast.order.dto.OrderResponse;
import com.project.pawcast.order.dto.OrderStatusRequest;
import com.project.pawcast.order.entity.Order;
import com.project.pawcast.order.entity.OrderItem;
import com.project.pawcast.order.entity.OrderStatus;
import com.project.pawcast.order.repository.OrderItemRepository;
import com.project.pawcast.order.repository.OrderRepository;
import com.project.pawcast.payment.entity.Payment;
import com.project.pawcast.payment.repository.PaymentRepository;
import com.project.pawcast.payment.service.PaymentService;
import com.project.pawcast.product.entity.ProductVariant;
import com.project.pawcast.product.repository.ProductRepository;
import com.project.pawcast.product.repository.ProductVariantRepository;
import com.project.pawcast.product.service.InventoryService;
import com.project.pawcast.refund.entity.Refund;
import com.project.pawcast.refund.service.RefundService;
import com.project.pawcast.security.AuthorizationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MembersRepository membersRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AuthorizationValidator validator;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final InventoryService inventoryService;

    // 주문 생성: 회원 확인 -> 옵션 비관적 락 조회/검증 -> 금액 계산 -> 주문 및 주문 항목 저장
    @Transactional
    public OrderResponse createOrder(Long memberId, OrderRequest request) {
        // 비회원 주문 허용을 위해 memberId가 있을 때만 회원 엔티티 조회
        Members member = null;
        if (memberId != null) {
            member = membersRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        }

        // 교착 상태 방지를 위해 상품 옵션 ID를 오름차순 정렬한 뒤 FOR UPDATE 락으로 조회
        List<Long> variantIds = request.items().stream()
                .map(OrderItemRequest::productVariantId)
                .sorted()
                .toList();
        List<ProductVariant> variants = productVariantRepository.findAllByIdForUpdate(variantIds);

        // 요청한 고유 옵션 수와 조회된 옵션 수 일치 여부 검증
        long distinctCount = variantIds.stream().distinct().count();
        if (variants.size() != distinctCount) {
            throw new BusinessException(ErrorCode.NOT_FOUND_OPTION);
        }
        if (variantIds.size() != distinctCount) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_REQUEST);
        }

        // O(1) 조회를 위해 옵션 ID 기준 Map 구성
        Map<Long, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, variant -> variant));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        for (OrderItemRequest item : request.items()) {
            ProductVariant variant = variantMap.get(item.productVariantId());
            if (variant == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_OPTION);
            }
            if (variant.getStock() < item.quantity()) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            }

            // 주문 시점의 옵션 단가 및 항목 합계 계산
            BigDecimal unitPrice = variant.getPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .productVariant(variant)
                    .productName(variant.getProduct().getName())
                    .optionLabel(variant.getOptionLabel())
                    .unitPrice(unitPrice)
                    .quantity(item.quantity())
                    .totalPrice(itemTotal)
                    .build();

            orderItems.add(orderItem);
        }

        // 20,000원 이상 무료 배송, 미만 시 3,000원 부과
        BigDecimal shippingFee = totalAmount.compareTo(BigDecimal.valueOf(20000)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(3000);
        BigDecimal finalAmount = totalAmount.add(shippingFee);

        Order order = Order.builder()
                .member(member)
                .orderNumber(OrderNumberGenerator.createOrderNumber())
                .status(OrderStatus.PENDING_PAYMENT)
                .recipientName(request.recipientName())
                .recipientPhone(request.recipientPhone())
                .recipientEmail(request.recipientEmail())
                .deliveryZipcode(request.deliveryZipcode())
                .deliveryAddress1(request.deliveryAddress1())
                .deliveryAddress2(request.deliveryAddress2())
                .deliveryMessage(request.deliveryMessage())
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .finalAmount(finalAmount)
                .build();

        orderRepository.save(order);
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        orderItemRepository.saveAll(orderItems);

        return OrderResponse.from(order, orderItems);
    }

    // 회원 주문 내역 목록 조회
    public List<OrderResponse> listOrder(Long memberId) {
        List<Order> orders = orderRepository.findAllByMember_IdOrderByCreatedAtDesc(memberId);
        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        // 주문 ID 목록으로 주문 항목을 일괄 조회해 메모리에서 주문별로 매핑
        List<OrderItem> items = orderIds.isEmpty()
                ? List.of()
                : orderItemRepository.findByOrder_IdIn(orderIds);
        Map<Long, List<OrderItem>> itemMap = items.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.stream()
                .map(order -> OrderResponse.from(order, itemMap.getOrDefault(order.getId(), List.of())))
                .toList();
    }

    // 주문 상세 조회 (회원은 memberId + orderId, 비회원은 이메일 + 주문번호 기준)
    public OrderResponse detailOrder(Long memberId, Long orderId, String email, String orderNumber) {
        if (memberId != null) {
            Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
            return OrderResponse.from(order, orderItemRepository.findAllByOrder_Id(orderId));
        }

        Order guestOrder = orderRepository.findByEmailAndOrderNumber(email, orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        return OrderResponse.from(guestOrder, orderItemRepository.findAllByOrder_Id(guestOrder.getId()));
    }

    // 회원 소유 주문 단건 조회 공통 메서드
    public Order findByIdAndMemberId(Long orderId, Long memberId) {
        return orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
    }

    // 주문 배송지 정보 수정 (결제 대기 또는 결제 완료 상태에서만 가능)
    @Transactional
    public OrderResponse updateOrder(Long orderId, Long memberId, OrderRequest request) {
        Order order = findByIdAndMemberId(orderId, memberId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.setRecipientName(request.recipientName());
        order.setRecipientPhone(request.recipientPhone());
        order.setRecipientEmail(request.recipientEmail());
        order.setDeliveryZipcode(request.deliveryZipcode());
        order.setDeliveryAddress1(request.deliveryAddress1());
        order.setDeliveryAddress2(request.deliveryAddress2());
        order.setDeliveryMessage(request.deliveryMessage());

        return OrderResponse.from(orderRepository.save(order), orderItemRepository.findAllByOrder_Id(orderId));
    }

    // 주문 취소: 주문 락 조회 -> 소유자 확인 -> 결제 취소 -> 환불 생성 -> 재고 및 판매량 복구
    @Transactional
    public void cancelOrder(Long orderId, Long memberId, String cancelReason) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        if (order.getMember() == null || !order.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_ORDER_OWNER);
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));

        String providerRefundKey = paymentService.cancelPayment(orderId, cancelReason);
        order.setStatus(OrderStatus.CANCELLED);
        refundService.createRefund(
                payment,
                cancelReason,
                providerRefundKey,
                Refund.RefundStatus.COMPLETED,
                LocalDateTime.now()
        );

        // 재고 복구 시 옵션 ID 기준으로 정렬해 순차 처리
        List<OrderItem> items = orderItemRepository.findAllByOrder_Id(order.getId())
                .stream()
                .sorted(Comparator.comparing(item -> item.getProductVariant().getId()))
                .toList();

        for (OrderItem item : items) {
            inventoryService.increaseStock(item.getProductVariant().getId(), item.getQuantity());
            productRepository.decreaseSalesCount(item.getProductVariant().getProduct().getId(), item.getQuantity());
        }
    }

    // 관리자/매니저용 전체 주문 목록 조회
    public List<OrderResponse> getOrderList(Long memberId) {
        Members member = membersRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);

        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        Map<Long, List<OrderItem>> itemMap = orderIds.isEmpty()
                ? Map.of()
                : orderItemRepository.findByOrder_IdIn(orderIds).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.stream()
                .map(order -> OrderResponse.from(order, itemMap.getOrDefault(order.getId(), List.of())))
                .toList();
    }

    // 관리자/매니저용 주문 상태 변경 (최초 배송 완료 전환 시 상품 판매량 반영)
    @Transactional
    public void changeOrderStatus(Long memberId, Long orderId, OrderStatusRequest request) {
        Members member = membersRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));

        if (order.getStatus() != OrderStatus.DELIVERED && request.orderStatus() == OrderStatus.DELIVERED) {
            List<OrderItem> orderItems = orderItemRepository.findAllByOrder_Id(orderId);
            for (OrderItem item : orderItems) {
                Long productId = item.getProductVariant().getProduct().getId();
                productRepository.increaseSalesCount(productId, item.getQuantity());
            }
        }

        order.setStatus(request.orderStatus());
    }
}
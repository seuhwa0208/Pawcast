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

    // 주문하기
    // 상품 검증 → 가격 계산 → 주문 생성 → 주문 상세 생성 → 재고 차감 → 결제 처리
    // 회원 확인하고 - 기반으로 카트불러오기 -
    //              - 바로 주문 창 -> // 결제 완료후 주문번호 이메일 발송?
    @Transactional
    public OrderResponse createOrder(Long memberId, OrderRequest request){
        // 회원 조회 - 비회원이면 null로 처리
        Members member = null;
        if (memberId != null){
            member = membersRepository.findById(memberId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        }

        // 상품 옵션 조회 -> 재고 여부와 판매 가능한 상태, 재고가 주문수량 이상인지 검사
        // 상품 옵션을 ID 오름차순으로 정렬 → FOR UPDATE 로 락
            // 상품 옵션 ID를 오름차순 정렬
        List<Long> variantIds = request.items().stream().map(OrderItemRequest::productVariantId).sorted().toList();
            //오름차순 정렬 순으로 상품 옵션 가져옴
        List<ProductVariant> variants = productVariantRepository.findAllByIdForUpdate(variantIds);
            // DB에서 검색한 옵션값과 방금 받은 Id 값(중복제거) 비교해서 제대로 요청된 옵션인지 확인
        if (variants.size() != variantIds.stream().distinct().count()) {throw new BusinessException(ErrorCode.NOT_FOUND_OPTION);}
            // 주문한 상품을 비교해서 중복된 상품을 막음 -> 하나로 주문해라
        if (variantIds.size() != variantIds.stream().distinct().count()) {throw new BusinessException(ErrorCode.INVALID_ORDER_REQUEST);}

        // 상품 가격 계산 / orderItem에는 가격*수량 금액을 / Order에는 총 금액
            // Map으로 변경 / options.stream().filter(found -> found.getId().equals(item.optionId())).findFirst()...
            // 위처럼 list도 되지만 매번 stream().filter를 사용하게됨
        Map<Long, ProductVariant> variantMap = variants.stream().collect(Collectors.toMap(ProductVariant::getId, variant -> variant));


        List<OrderItem> orderItems = new ArrayList<>(); //
        BigDecimal totalAmount = BigDecimal.ZERO;   // 총액
        // 현재 가격 정책에는 별도 주문 할인이 없습니다.
        BigDecimal discountAmount = BigDecimal.ZERO;

            // request에 들은 아이템 만큼 orderItem이 반복
        for (OrderItemRequest item : request.items()) {
            ProductVariant variant = variantMap.get(item.productVariantId());
            // 옵션 확인
            if (variant == null) {throw new BusinessException(ErrorCode.NOT_FOUND_OPTION);}
            // 재고 확인
            if (variant.getStock() < item.quantity()) {throw new BusinessException(ErrorCode.OUT_OF_STOCK);}

            // 상품 조회와 동일한 최종 판매가를 주문 시점 단가로 저장
            BigDecimal unitPrice = variant.getPrice();

            // 옵션별 상품 금액 / 가격 * 수량 = totalPrice
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
            
            // 모든 주문 금액 총액 계산
            totalAmount = totalAmount.add(itemTotal);

            // 주문 상품 생성 / OrderItem
            OrderItem orderItem = OrderItem.builder()
                    .productVariant(variant)
                    .productName(variant.getProduct().getName())
                    .optionLabel(variant.getOptionLabel())
                    .unitPrice(unitPrice)
                    .quantity(item.quantity())
                    .totalPrice(itemTotal)
                    .build();

            orderItems.add(orderItem);
            }// for문 끝 / OrderItem 주문 상품 목록 끝
        // 최종금액 및 배달비 및 할인 금액 계산 / for문 위에 있음
            //배달비
        BigDecimal shippingFee = totalAmount.compareTo(BigDecimal.valueOf(20000)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(3000);
            // 최종 금액 / 총액 + 배달비
        BigDecimal finalAmount = totalAmount.add(shippingFee);

        //Order를 생성하고 ->
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

        // 저장
        orderRepository.save(order);
        // orderItem들 order 값 저장
        for (OrderItem item : orderItems) {item.setOrder(order);}
        orderItemRepository.saveAll(orderItems);
        return OrderResponse.from(order);
    }

    // 주문 내역(목록) 조회
    public List<OrderResponse> listOrder(Long memberId){
        return orderRepository.findAllByMember_IdOrderByCreatedAtDesc(memberId).stream().map(OrderResponse::from).toList();
    }

    // 주문 정보(상세) 조회( 회원 비회원 - 이메일과 주문번호)
    public OrderResponse detailOrder(Long memberId, Long orderId, String email, String orderNumber){
        // 회원은 - 회원 아이디로 조회 / 비회원은 이메일 + 주문번호
        if (memberId != null){return OrderResponse.from(orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND_ORDER)));}
        // 비회원
        return OrderResponse.from(orderRepository.findByEmailAndOrderNumber(email, orderNumber)
                .orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND_ORDER)));
    }
    
    // 공통 - orderId 와 memberId로 조회
    public Order findByIdAndMemberId(Long orderId, Long memberId){
        return orderRepository.findByIdAndMemberId(orderId, memberId).orElseThrow(()-> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
    }

    // 수정 - 결제 완료 되면 수정 불가 - 배송지 수정
    @Transactional
    public OrderResponse updateOrder(Long orderId, Long memberId, OrderRequest request){
        Order order = findByIdAndMemberId(orderId, memberId);
        // 주문 상태 검사
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAID){
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        // 결제 완료는 옵션은 못바꿈 - 취소후 다시 재주문
        order.setRecipientName(request.recipientName());
        order.setRecipientPhone(request.recipientPhone());
        order.setRecipientEmail(request.recipientEmail());
        order.setDeliveryZipcode(request.deliveryZipcode());
        order.setDeliveryAddress1(request.deliveryAddress1());
        order.setDeliveryAddress2(request.deliveryAddress2());
        order.setDeliveryMessage(request.deliveryMessage());

        return OrderResponse.from(orderRepository.save(order));
    }

    // 삭제 - 소프트삭제 - 삭제할 이유는 보관 기간이 만료되어서 삭제 될때말곤 없어서 필요없는 기능임
//    @Transactional
//    public void deleteOrder(Long orderId, Long memberId){
//        Order order = findByIdAndMemberId(orderId, memberId);
//        order.setDeletedAt(LocalDateTime.now());
//    }

    // 주문 취소하기 - 이용자가 직접함 - 회원정보 일치하는지 확인
    // 결제 완료(배송 대기중 상태로 넘어가기 전에) -> 결제 취소 -> 환불 정보
    @Transactional
    public void cancelOrder(Long orderId, Long memberId, String cancelReason){
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));
        if (order.getMember() == null || !order.getMember().getId().equals(memberId)) {throw new BusinessException(ErrorCode.NOT_ORDER_OWNER);}
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PAYMENT));
        //결제 취소
        String  providerRefundKey = paymentService.cancelPayment(orderId,cancelReason);
        // 상품 상태 변경 - 취소함
        order.setStatus(OrderStatus.CANCELLED);
        // 환불 정보 만들고 -> 완료 날짜 새겨줘야함
        refundService.createRefund(payment,cancelReason, providerRefundKey, Refund.RefundStatus.COMPLETED , LocalDateTime.now());
        // 재고 검사
        List<OrderItem> items = orderItemRepository.findAllByOrder_Id(order.getId())
                .stream()
                .sorted(Comparator.comparing(item -> item.getProductVariant().getId()))
                .toList();
        // 재고 증가 및 판매량 감소
        for (OrderItem item : items){
            inventoryService.increaseStock(item.getProductVariant().getId(), item.getQuantity());
            productRepository.decreaseSalesCount(item.getProductVariant().getProduct().getId(), item.getQuantity());
        }
    }

    // 취소 요청의 진행상태? 및 환불 정보 조회 - 취소된 목록X 취소된 하나의 정보 불러오는것


    //전체 목록 띄우고
    public List<OrderResponse> getOrderList(Long memberId){
        // 권한 검사
        Members member = membersRepository.findById(memberId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
    }

    // 주문 상태 변경 (관리자 및 매니저) 및 배송 완료되면 판매량 증가 + 1
    @Transactional
    public void changeOrderStatus(Long memberId, Long orderId, OrderStatusRequest request) {
        // 권한 검사
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        validator.validateManager(member);

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ORDER));

        // 배송 완료로 변경되는 경우 판매량 증가
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

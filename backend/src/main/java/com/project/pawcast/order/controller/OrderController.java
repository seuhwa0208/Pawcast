package com.project.pawcast.order.controller;

import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.order.dto.CancelOrderRequest;
import com.project.pawcast.order.dto.OrderRequest;
import com.project.pawcast.order.dto.OrderResponse;
import com.project.pawcast.order.dto.OrderStatusRequest;
import com.project.pawcast.order.service.OrderService;
import com.project.pawcast.security.AuthorizationValidator;
import com.project.pawcast.security.LoginMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.checkerframework.checker.units.qual.A;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final AuthorizationValidator validator;
    private final MembersRepository membersRepository;

    // 주문하기
    @PostMapping
    public OrderResponse createOrder(@AuthenticationPrincipal LoginMember member, @Valid @RequestBody OrderRequest request){
       return orderService.createOrder(member == null ? null : member.getId(), request);
    }
    
    //회원의 주문 내역 조회
    @GetMapping
    public List<OrderResponse> listOrder(@AuthenticationPrincipal LoginMember member){
        return orderService.listOrder(member.getId());
    }
    
    //주문 정보 조회
    @GetMapping("/{orderId}")
    public OrderResponse detailOrder(@AuthenticationPrincipal LoginMember member, @PathVariable Long orderId, String email, String orderNumber){
        return orderService.detailOrder(member.getId(),orderId,email,orderNumber);
    }

    // 비회원 주문 조회: 주문 시 입력한 이메일과 서버가 발급한 주문번호를 함께 확인한다.
    @GetMapping("/guest")
    public OrderResponse detailGuestOrder(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String orderNumber
    ) {
        return orderService.detailGuestOrder(email, orderNumber);
    }

    // 주문 정보 수정 - 배송지 수정
    @PatchMapping("/{orderId}")
    public OrderResponse updateOrder(@AuthenticationPrincipal LoginMember member, @PathVariable Long orderId,@Valid @RequestBody OrderRequest request){
        return orderService.updateOrder(orderId, member.getId(),request);
    }

    //주문 취소 - 상태 캔슬 및 결제 취소
    @PostMapping("/{orderId}/cancel")
    public void cancelOrder(@AuthenticationPrincipal LoginMember member, @PathVariable Long orderId, @RequestBody CancelOrderRequest request){
        orderService.cancelOrder(orderId, member.getId(),request.cancelReason());
    }

    // 취소 요청의 진행상태 및 환불 정보 조회 - 취소된 주문 정보 조회 - 제외
    //@GetMapping("/{orderId}/cancel/{cancelId}")

    // 전체 주문 목록
    @GetMapping("/admin")
    public List<OrderResponse> getOrderList(@AuthenticationPrincipal LoginMember member){
        return orderService.getOrderList(member.getId());

    }
    // 상태 변경 - 주문
    @PostMapping("/admin/{orderId}/status")
    public OrderResponse changeOrderStatus(@AuthenticationPrincipal LoginMember member, @PathVariable Long orderId, @Valid @RequestBody OrderStatusRequest request){
        return orderService.changeOrderStatus(member.getId(),orderId,request);
    }
}

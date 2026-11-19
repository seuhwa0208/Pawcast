package com.project.pawcast.order.controller;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.order.dto.OrderRequest;
import com.project.pawcast.order.dto.OrderResponse;
import com.project.pawcast.order.dto.OrderStatusRequest;
import com.project.pawcast.order.service.OrderService;
import com.project.pawcast.security.AuthorizationValidator;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.checkerframework.checker.units.qual.A;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final AuthorizationValidator validator;
    private final MembersRepository membersRepository;

    // 주문하기
    @PostMapping
    public OrderResponse createOrder(@AuthenticationPrincipal LoginMember member, @Valid @RequestBody OrderRequest request){
       return orderService.createOrder(member.getId(), request);
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

    //주문 취소 - 소프트 삭제
    @PostMapping("/{orderId}/cancel")
    public void cancelOrder(@AuthenticationPrincipal LoginMember member, @PathVariable Long orderId){
        orderService.cancelOrder(orderId, member.getId());
    }
    
    
    // 취소 요청의 진행상태 및 환불 정보 조회 - 취소된 주문 정보 조회 - 제외
    //@GetMapping("/{orderId}/cancel/{cancelId}")

    // 전체 주문 목록
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<OrderResponse> getOrderList(@AuthenticationPrincipal LoginMember member){
        return orderService.getOrderList(member.getId());

    }
    // 상태 변경 - 주문
    @PostMapping("/admin/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void changeOrderStatus(@AuthenticationPrincipal LoginMember member, @PathVariable Long orderId, @RequestBody OrderStatusRequest request){
        orderService.changeOrderStatus(member.getId(),orderId,request);
    }
}

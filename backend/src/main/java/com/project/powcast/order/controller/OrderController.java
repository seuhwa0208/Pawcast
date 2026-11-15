package com.project.powcast.order.controller;

import com.project.powcast.order.dto.OrderRequest;
import com.project.powcast.order.dto.OrderResponse;
import com.project.powcast.order.service.OrderService;
import com.project.powcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    // 주문하기
    @PostMapping
    public OrderResponse createOrder(@AuthenticationPrincipal LoginMember member, OrderRequest request){
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
}

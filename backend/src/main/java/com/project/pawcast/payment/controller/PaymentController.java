package com.project.pawcast.payment.controller;

import com.project.pawcast.payment.dto.PaymentDtos;
import com.project.pawcast.payment.service.PaymentService;
import com.project.pawcast.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 하기 - 승인
    @PostMapping("/confirm")
    public PaymentDtos.PaymentResponse approve(@PathVariable Long orderId, @AuthenticationPrincipal LoginMember member, @RequestBody @Valid PaymentDtos.PaymentRequest request) {
        return paymentService.approve(orderId, member.getId(), request.paymentKey(), request.orderNumber(), request.amount());
    }

    // 결제 조회
    @GetMapping
    public PaymentDtos.PaymentResponse detailPayment(@PathVariable Long orderId ,@AuthenticationPrincipal LoginMember member){
        return paymentService.detailPayment(orderId,member.getId());
    }
}

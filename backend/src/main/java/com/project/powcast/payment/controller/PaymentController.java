package com.project.powcast.payment.controller;

import com.project.powcast.payment.dto.PaymentDtos;
import com.project.powcast.payment.service.PaymentService;
import com.project.powcast.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 승인
    @PostMapping("/confirm")
    public PaymentDtos.PaymentResponse approve(@PathVariable Long orderId, @AuthenticationPrincipal LoginMember member, @RequestBody @Valid PaymentDtos.PaymentRequest request) {

        PaymentDtos.PaymentResponse response = paymentService.approve(
                        orderId,
                        member.getId(),
                        request.paymentKey(),
                        request.orderNumber(),
                        request.amount()
                );
        return response;
    }
    // 결제 조회
    @GetMapping
    public PaymentDtos.PaymentResponse detailPayment(@PathVariable Long orderId ,@AuthenticationPrincipal LoginMember member){
        return paymentService.detailPayment(orderId,member.getId());
    }
    
    // 결제 취소
    @PostMapping("/cancel")
    public void cancelPayment(@PathVariable Long orderId, @AuthenticationPrincipal LoginMember member, String reason){
        paymentService.cancelPayment(orderId,member.getId(),reason);
    }
}

package com.project.pawcast.refund.controller;

import com.project.pawcast.refund.dto.RefundCancelReasonRequest;
import com.project.pawcast.refund.dto.RefundResponse;
import com.project.pawcast.refund.service.RefundService;
import com.project.pawcast.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders/{orderId}/refunds")
public class RefundController {

    private final RefundService refundService;

    //환불 내역 조회
    @GetMapping
    public RefundResponse detailRefund(@PathVariable Long orderId, @AuthenticationPrincipal LoginMember member){
        return refundService.detailRefund(orderId, member.getId());
    }
    
    //환불 요청 기능
    @PostMapping
    public void refundPayment(@PathVariable Long orderId, @AuthenticationPrincipal LoginMember member,@Valid @RequestBody RefundCancelReasonRequest request){
        refundService.refundPayment(orderId, member.getId(), request.cancelReason());
    }
}

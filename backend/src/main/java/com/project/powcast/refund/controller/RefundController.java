package com.project.powcast.refund.controller;

import com.project.powcast.refund.dto.RefundResponse;
import com.project.powcast.refund.service.RefundService;
import com.project.powcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

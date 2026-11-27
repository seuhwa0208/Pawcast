package com.project.pawcast.refund.controller;

import com.project.pawcast.refund.dto.RefundResponse;
import com.project.pawcast.refund.service.RefundService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/refunds")
public class AdminRefundController {
    private final RefundService refundService;

    @GetMapping
    public List<RefundResponse> list(@AuthenticationPrincipal LoginMember member) {
        return refundService.listRefundsForManager(member.getId());
    }

    @PatchMapping("/{refundId}/approve")
    public void approve(
            @AuthenticationPrincipal LoginMember member,
            @PathVariable Long refundId) {
        refundService.approveRefund(refundId, member.getId());
    }

    @PatchMapping("/{refundId}/reject")
    public void reject(
            @AuthenticationPrincipal LoginMember member,
            @PathVariable Long refundId) {
        refundService.rejectRefund(refundId, member.getId());
    }
}

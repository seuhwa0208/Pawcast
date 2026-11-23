package com.project.pawcast.member.admin.controller;

import com.project.pawcast.member.admin.dto.MemberRoleRequest;
import com.project.pawcast.member.admin.dto.MemberStatusRequest;
import com.project.pawcast.member.admin.service.AdminService;
import com.project.pawcast.refund.service.RefundService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.project.pawcast.member.dto.MemberListResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final RefundService refundService;
    
    // 회원 전체목록 - 강아지수 고양이수 반영
    @GetMapping
    public List<MemberListResponse> getMemberList(@AuthenticationPrincipal LoginMember member){
        return adminService.getMemberList(member.getId());
    }


    public void changeMemberStatus(@AuthenticationPrincipal LoginMember member, @PathVariable Long userId, @RequestBody MemberStatusRequest request){
        adminService.changeMemberStatus(member.getId(),userId, request);
    }
    // 회원 역할 변경
    @PatchMapping("/members/{userId}/role")
    public void changeMemberRole(@AuthenticationPrincipal LoginMember member, @PathVariable Long userId, @RequestBody MemberRoleRequest request){
        adminService.changeMemberRole(member.getId(),userId, request);
    }

    // 환불 승인
    @PatchMapping("/refunds/{refundId}/approve")
    public void approveRefund(@PathVariable Long refundId, @AuthenticationPrincipal LoginMember member) {
        refundService.approveRefund(refundId, member.getId());
    }
}

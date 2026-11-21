package com.project.pawcast.member.admin.controller;

import com.project.pawcast.member.admin.dto.MemberRoleRequest;
import com.project.pawcast.member.admin.dto.MemberRoleResponse;
import com.project.pawcast.member.admin.dto.MemberStatusRequest;
import com.project.pawcast.member.admin.service.AdminService;
import com.project.pawcast.member.admin.service.AdminDashboardService;
import com.project.pawcast.member.dto.MemberListResponse;
import com.project.pawcast.member.dto.MemberResponse;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.refund.service.RefundService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final RefundService refundService;
    private final AdminDashboardService adminDashboardService;

    /**
     * 브라우저는 AI 서버에 직접 접속하지 않고 Spring 인증을 거쳐 매출 집계를 받는다.
     * AI 재학습은 별도 /api/admin/ai 경로에서만 서버 간 내부 키로 호출한다.
     */
    @GetMapping("/dashboard/sales")
    public Map<String, Object> getSalesDashboard(
            @AuthenticationPrincipal LoginMember member,
            @RequestParam(defaultValue = "month") String unit,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return adminDashboardService.getSalesDashboard(member.getId(), unit, year, from, to);
    }

    @GetMapping("/dashboard/sales/years")
    public List<Integer> getSalesYears(@AuthenticationPrincipal LoginMember member) {
        return adminDashboardService.getAvailableSalesYears(member.getId());
    }

    @GetMapping("/dashboard/sales/export")
    public ResponseEntity<byte[]> exportSalesDashboard(
            @AuthenticationPrincipal LoginMember member,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "all") String section) {
        var file = adminDashboardService.exportSalesStatistics(member.getId(), startDate, endDate, format, section);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, file.disposition() == null ? "attachment" : file.disposition())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
    
    // 회원 전체목록 - 강아지수 고양이수 반영
    @GetMapping
    public List<MemberListResponse> getMemberList(@AuthenticationPrincipal LoginMember member){
        return adminService.getMemberList(member.getId());
    }


    @PatchMapping("/members/{userId}/status")
    public void changeMemberStatus(@AuthenticationPrincipal LoginMember member, @PathVariable Long userId, @RequestBody MemberStatusRequest request){
        adminService.changeMemberStatus(member.getId(),userId, request);
    }
    // 회원 역할 변경
    @PatchMapping("/members/{userId}/role")
    public void changeMemberRole(@AuthenticationPrincipal LoginMember member, @PathVariable Long userId, @RequestBody MemberRoleRequest request){
        adminService.changeMemberRole(member.getId(),userId, request);
    }

    // 환불 승인
    @GetMapping("/refunds")
    public java.util.List<com.project.pawcast.refund.dto.RefundResponse> getRefundList(
            @AuthenticationPrincipal LoginMember member) {
        return refundService.listRefundsForManager(member.getId());
    }

    @PatchMapping("/refunds/{refundId}/approve")
    public void approveRefund(@PathVariable Long refundId, @AuthenticationPrincipal LoginMember member) {
        refundService.approveRefund(refundId, member.getId());
    }
}

package com.project.pawcast.member.admin.controller;

import com.project.pawcast.member.admin.service.AdminDashboardService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


/** 관리 화면 전용의 다중 구간 매출 집계와 AI 재학습 진입점입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    /** 주문 데이터가 존재하는 연도만 반환해 관리자 매출 조회 선택지를 DB와 동기화한다. */
    @GetMapping("/sales/years")
    public List<Integer> salesYears(@AuthenticationPrincipal LoginMember member) {
        return adminDashboardService.getAvailableSalesYears(member.getId());
    }

}

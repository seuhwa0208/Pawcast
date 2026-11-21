package com.project.pawcast.member.admin.controller;

import com.project.pawcast.member.admin.service.AdminDashboardService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** AI 서버 호출을 브라우저에 노출하지 않고 관리자 권한으로 중계한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ai")
public class AdminAiController {
    private final AdminDashboardService adminDashboardService;

    @PostMapping("/sales/retrain")
    public Map<String, Object> retrainSales(@AuthenticationPrincipal LoginMember member,
                                             @RequestParam(required = false) LocalDate forecastDate) {
        return adminDashboardService.reforecastSales(member.getId(), forecastDate == null ? LocalDate.now() : forecastDate);
    }
}

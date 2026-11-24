package com.project.pawcast.member.admin.controller;

import com.project.pawcast.member.admin.service.AdminDashboardService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 관리 화면 전용의 다중 구간 매출 집계와 AI 재학습 진입점입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/sales")
    public Map<String, Object> sales(
            @AuthenticationPrincipal LoginMember member,
            @RequestParam(defaultValue = "month") String unit,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return adminDashboardService.getSalesDashboard(member.getId(), unit, year, from, to);
    }

    /** 주문 데이터가 존재하는 연도만 반환해 관리자 매출 조회 선택지를 DB와 동기화한다. */
    @GetMapping("/sales/years")
    public List<Integer> salesYears(@AuthenticationPrincipal LoginMember member) {
        return adminDashboardService.getAvailableSalesYears(member.getId());
    }

    @PostMapping("/sales/retrain")
    public Map<String, Object> retrain(
            @AuthenticationPrincipal LoginMember member,
            @RequestParam(required = false) LocalDate forecastDate) {
        return adminDashboardService.reforecastSales(member.getId(), forecastDate == null ? LocalDate.now() : forecastDate);
    }

    /**
     * AI가 생성한 통계 파일을 Spring이 전달한다.
     * 브라우저가 FastAPI에 직접 JWT를 검증시키지 않아도 되므로 관리자 로그인 토큰 체계를 하나로 유지한다.
     */
    @GetMapping("/sales/export")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal LoginMember member,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(defaultValue = "all") String section) {
        var file = adminDashboardService.exportSalesStatistics(
                member.getId(), startDate, endDate, format, section);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(file.contentType());
        } catch (Exception ignored) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, file.disposition())
                .body(file.content());
    }

    @GetMapping("/overview/review-sentiment")
    public Map<String, Object> reviewSentiment(
            @AuthenticationPrincipal LoginMember member,
            @RequestParam(defaultValue = "5") int limit) {
        return adminDashboardService.getReviewSentimentSummary(member.getId(), limit);
    }

    @GetMapping("/overview/inventory-forecast")
    public Map<String, Object> inventoryForecast(
            @AuthenticationPrincipal LoginMember member,
            @RequestParam(defaultValue = "5") int limit) {
        return adminDashboardService.getInventoryForecast(member.getId(), limit);
    }
}

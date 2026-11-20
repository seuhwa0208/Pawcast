package com.project.pawcast.member.admin.dto;

import java.math.BigDecimal;

public record SalesResponse(
        String period,             // 통계 기간
        BigDecimal totalSales,     // 총매출액
        Long orderCount,           // 주문 건수
        BigDecimal averageOrderAmount, // 평균 주문 금액
        BigDecimal cancelAmount    // 취소 금액
) {}

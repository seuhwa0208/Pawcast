package com.project.pawcast.member.admin.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.security.AuthorizationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final String SALES_STATUSES = "'PAID','PREPARING','SHIPPING','DELIVERED'";

    private final MembersRepository membersRepository;
    private final AuthorizationValidator authorizationValidator;
    private final NamedParameterJdbcTemplate jdbc;

    public Map<String, Object> getSalesDashboard(
            Long adminId, String unit, Integer year, LocalDate from, LocalDate to) {
        Members admin = membersRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        authorizationValidator.validateManager(admin);

        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        LocalDate start = from == null ? LocalDate.of(selectedYear, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.of(selectedYear, 12, 31) : to;
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", start)
                .addValue("end", end);

        String totalsSql = """
                SELECT
                    COALESCE(SUM(final_amount) FILTER (WHERE status IN (%s)), 0) AS total_sales,
                    COUNT(*) FILTER (WHERE status IN (%s)) AS total_orders,
                    COALESCE(AVG(final_amount) FILTER (WHERE status IN (%s)), 0) AS average_order,
                    COALESCE(SUM(final_amount) FILTER (WHERE status = 'CANCELLED'), 0) AS refund_amount
                FROM orders
                WHERE created_at::date BETWEEN :start AND :end
                """.formatted(SALES_STATUSES, SALES_STATUSES, SALES_STATUSES);
        Map<String, Object> totals = jdbc.queryForMap(totalsSql, params);

        String truncUnit = switch (unit == null ? "month" : unit.toLowerCase()) {
            case "day" -> "day";
            case "quarter" -> "quarter";
            case "year" -> "year";
            default -> "month";
        };
        String trendSql = """
                SELECT TO_CHAR(DATE_TRUNC('%s', created_at), '%s') AS label,
                       COALESCE(SUM(final_amount), 0) AS sales,
                       COUNT(*) AS orders
                FROM orders
                WHERE created_at::date BETWEEN :start AND :end
                  AND status IN (%s)
                GROUP BY DATE_TRUNC('%s', created_at)
                ORDER BY DATE_TRUNC('%s', created_at)
                """.formatted(truncUnit, labelFormat(truncUnit), SALES_STATUSES, truncUnit, truncUnit);
        List<Map<String, Object>> trendRows = jdbc.queryForList(trendSql, params);

        // 관리자 첫 화면은 하위 식품 분류가 아닌 쇼핑몰 3개 관(DOG/CAT/DEVICE)만 비교한다.
        // 세부 사료·간식·식기 분류는 상품 관리와 다운로드 파일에서 별도로 제공한다.
        String categorySql = """
                SELECT CASE p.product_type
                           WHEN 'DOG' THEN '강아지관'
                           WHEN 'CAT' THEN '고양이관'
                           WHEN 'DEVICE' THEN '식기·급수관'
                           ELSE '기타'
                       END AS name,
                       COALESCE(SUM(oi.total_price), 0) AS sales
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                JOIN product_variants pv ON pv.id = oi.product_variant_id
                JOIN products p ON p.id = pv.product_id
                WHERE o.created_at::date BETWEEN :start AND :end
                  AND o.status IN (%s)
                GROUP BY p.product_type
                ORDER BY sales DESC
                """.formatted(SALES_STATUSES);
        List<Map<String, Object>> categoryRows = jdbc.queryForList(categorySql, params);

        String topProductsSql = """
                SELECT oi.product_name AS name,
                       COALESCE(SUM(oi.total_price), 0) AS sales,
                       COALESCE(SUM(oi.quantity), 0) AS quantity
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.created_at::date BETWEEN :start AND :end
                  AND o.status IN (%s)
                GROUP BY oi.product_name
                ORDER BY sales DESC
                LIMIT 5
                """.formatted(SALES_STATUSES);
        List<Map<String, Object>> topProducts = jdbc.queryForList(topProductsSql, params);

        BigDecimal totalSales = decimal(totals.get("total_sales"));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalSales", totalSales);
        summary.put("totalOrders", ((Number) totals.get("total_orders")).longValue());
        summary.put("averageOrderAmount", decimal(totals.get("average_order")).setScale(0, RoundingMode.HALF_UP));
        summary.put("refundAmount", decimal(totals.get("refund_amount")));

        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("labels", trendRows.stream().map(row -> row.get("label")).toList());
        trend.put("sales", trendRows.stream().map(row -> row.get("sales")).toList());
        trend.put("orders", trendRows.stream().map(row -> row.get("orders")).toList());

        // AI 예측은 Vue가 FastAPI에서 직접 조회한다. 이 Spring 응답은 일반 DB 집계만 담당한다.
        Map<String, Object> forecast = new LinkedHashMap<>();
        forecast.put("predictedSales", null);
        forecast.put("growthRate", null);
        forecast.put("insight", "AI 예측은 FastAPI에서 직접 제공합니다.");
        forecast.put("topProducts", topProducts);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("trend", trend);
        response.put("categories", categoryRows);
        response.put("forecast", forecast);
        return response;
    }

    /** 주문 데이터가 실제로 존재하는 연도만 조회 조건에 제공한다. */
    public List<Integer> getAvailableSalesYears(Long adminId) {
        Members admin = membersRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        authorizationValidator.validateManager(admin);
        return jdbc.queryForList("""
                SELECT DISTINCT EXTRACT(YEAR FROM created_at)::int AS sales_year
                FROM orders
                WHERE created_at IS NOT NULL
                ORDER BY sales_year DESC
                """, new MapSqlParameterSource(), Integer.class);
    }

    private String labelFormat(String unit) {
        return switch (unit) {
            case "day" -> "YYYY-MM-DD";
            case "quarter" -> "YYYY \"Q\"Q";
            case "year" -> "YYYY";
            default -> "YYYY-MM";
        };
    }

    private BigDecimal decimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }
}

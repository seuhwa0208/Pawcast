package com.project.pawcast.member.admin.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.admin.client.SalesForecastClient;
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
    private final SalesForecastClient salesForecastClient;

    public Map<String, Object> getSalesDashboard(
            Long adminId, String unit, Integer year, LocalDate from, LocalDate to) {
        Members admin = membersRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        authorizationValidator.validateManager(admin);

        int selectedYear = year == null ? LocalDate.now().getYear() : year;
        LocalDate start = from == null ? LocalDate.of(selectedYear, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.of(selectedYear, 12, 31) : to;
        if (end.isBefore(start)) throw new BusinessException(ErrorCode.INVALID_REQUEST);

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

        String categorySql = """
                SELECT COALESCE(c.name, '기타') AS name,
                       COALESCE(SUM(oi.total_price), 0) AS sales
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                JOIN product_variants pv ON pv.id = oi.product_variant_id
                JOIN products p ON p.id = pv.product_id
                LEFT JOIN categories c ON c.id = p.category_id
                WHERE o.created_at::date BETWEEN :start AND :end
                  AND o.status IN (%s)
                GROUP BY COALESCE(c.name, '기타')
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

        Map<String, Object> forecast = new LinkedHashMap<>();
        forecast.put("predictedSales", totalSales.multiply(BigDecimal.valueOf(1.05)).setScale(0, RoundingMode.HALF_UP));
        forecast.put("growthRate", 5.0);
        forecast.put("insight", ((Number) totals.get("total_orders")).longValue() == 0
                ? "선택 기간의 결제 데이터가 없습니다."
                : "최근 매출 흐름을 기준으로 한 단순 예상치입니다.");
        forecast.put("topProducts", topProducts);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary", summary);
        response.put("trend", trend);
        response.put("categories", categoryRows);
        response.put("forecast", forecast);
        return response;
    }

    public Map<String, Object> reforecastSales(Long adminId, LocalDate forecastDate) {
        Members admin = membersRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        authorizationValidator.validateManager(admin);
        SalesForecastClient.ForecastResponse result = salesForecastClient.retrain(forecastDate);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("predictedSales", result.predicted_sales());
        response.put("growthRate", result.growth_rate());
        response.put("forecastDate", result.forecast_date());
        response.put("trainedAt", result.trained_at());
        response.put("trainingDays", result.training_days());
        response.put("insight", result.insight());
        return response;
    }

    private String labelFormat(String unit) {
        return switch (unit) {
            case "day" -> "YYYY-MM-DD";
            case "year" -> "YYYY";
            default -> "YYYY-MM";
        };
    }

    private BigDecimal decimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }
}

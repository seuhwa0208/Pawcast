"""실제 매출 통계를 조회하고 CSV·Excel 다운로드 파일로 변환합니다."""

from datetime import date, timedelta
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

import pandas as pd
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.sales.service import retrain_sales_forecast


SCHEMA_PATH = Path(__file__).resolve().parents[3] / "shared" / "export-schemas" / "sales.json"


def _load_export_schema() -> dict:
    """Vue와 동일한 JSON 정의로 파일명·시트명·열 헤더를 통일한다."""
    with SCHEMA_PATH.open(encoding="utf-8") as schema_file:
        return json.load(schema_file)


EXPORT_SCHEMA = _load_export_schema()
SECTION_SCHEMA = {
    section["key"]: section
    for section in EXPORT_SCHEMA["sections"]
    if section["key"] != "all"
}


async def _query_frame(
    session: AsyncSession,
    statement: str,
    parameters: dict,
    columns: list[str],
) -> pd.DataFrame:
    """SQL 조회 결과가 0건이어도 정해진 컬럼을 가진 표를 반환합니다."""
    result = await session.execute(text(statement), parameters)
    return pd.DataFrame([dict(row) for row in result.mappings().all()], columns=columns)


async def load_sales_export_frames(
    session: AsyncSession,
    start_date: date,
    end_date: date,
) -> dict[str, pd.DataFrame]:
    """화면과 파일에서 공통으로 사용할 실제 DB 매출 통계를 조회합니다."""
    if start_date > end_date:
        raise ValueError("시작일은 종료일보다 늦을 수 없습니다.")

    # 조회 기간과 동일한 길이의 직전 기간을 만들어 증감률을 계산합니다.
    period_days = (end_date - start_date).days + 1
    previous_start = start_date - timedelta(days=period_days)
    parameters = {
        "start_date": start_date,
        "end_exclusive": end_date + timedelta(days=1),
        "previous_start": previous_start,
    }

    summary = await _query_frame(
        session,
        """
        WITH current_sales AS (
            SELECT COALESCE(SUM(final_amount), 0) AS total_sales,
                   COUNT(*) AS total_orders,
                   COALESCE(AVG(final_amount), 0) AS average_order_amount
            FROM orders
            WHERE created_at >= :start_date AND created_at < :end_exclusive
              AND deleted_at IS NULL
              AND status IN ('PAID','PREPARING','SHIPPING','DELIVERED')
        ), previous_sales AS (
            SELECT COALESCE(SUM(final_amount), 0) AS total_sales,
                   COUNT(*) AS total_orders
            FROM orders
            WHERE created_at >= :previous_start AND created_at < :start_date
              AND deleted_at IS NULL
              AND status IN ('PAID','PREPARING','SHIPPING','DELIVERED')
        ), refund_total AS (
            SELECT COALESCE(SUM(r.refund_amount), 0) AS refund_amount
            FROM refunds r JOIN payments p ON p.id = r.payment_id
            JOIN orders o ON o.id = p.order_id
            WHERE o.created_at >= :start_date AND o.created_at < :end_exclusive
              AND r.status = 'COMPLETED'
        )
        SELECT c.total_sales, c.total_orders,
               ROUND(c.average_order_amount)::bigint AS average_order_amount,
               r.refund_amount,
               CASE WHEN p.total_sales = 0 THEN 0
                    ELSE ROUND((c.total_sales-p.total_sales)/p.total_sales*100, 1) END AS sales_growth_rate,
               CASE WHEN p.total_orders = 0 THEN 0
                    ELSE ROUND((c.total_orders-p.total_orders)::numeric/p.total_orders*100, 1) END AS order_growth_rate
        FROM current_sales c CROSS JOIN previous_sales p CROSS JOIN refund_total r
        """,
        parameters,
        ["total_sales", "total_orders", "average_order_amount", "refund_amount", "sales_growth_rate", "order_growth_rate"],
    )

    daily = await _query_frame(
        session,
        """
        SELECT created_at::date AS sale_date,
               COALESCE(SUM(final_amount), 0) AS sales,
               COUNT(*) AS orders,
               ROUND(AVG(final_amount))::bigint AS average_order_amount
        FROM orders
        WHERE created_at >= :start_date AND created_at < :end_exclusive
          AND deleted_at IS NULL
          AND status IN ('PAID','PREPARING','SHIPPING','DELIVERED')
        GROUP BY created_at::date ORDER BY sale_date
        """,
        parameters,
        ["sale_date", "sales", "orders", "average_order_amount"],
    )

    categories = await _query_frame(
        session,
        """
        WITH product_group AS (
            SELECT p.id AS product_id,
                   CASE p.product_type
                        WHEN 'DOG' THEN '강아지관'
                        WHEN 'CAT' THEN '고양이관'
                        WHEN 'DEVICE' THEN '식기·급수관'
                        ELSE '기타'
                   END AS category_name
            FROM products p
        ), current_period AS (
            SELECT pg.category_name, SUM(oi.total_price) AS sales,
                   COUNT(DISTINCT o.id) AS orders
            FROM orders o JOIN order_items oi ON oi.order_id = o.id
            JOIN product_variants pv ON pv.id = oi.product_variant_id
            JOIN product_group pg ON pg.product_id = pv.product_id
            WHERE o.created_at >= :start_date AND o.created_at < :end_exclusive
              AND o.deleted_at IS NULL
              AND o.status IN ('PAID','PREPARING','SHIPPING','DELIVERED')
            GROUP BY pg.category_name
        ), previous_period AS (
            SELECT pg.category_name, SUM(oi.total_price) AS sales
            FROM orders o JOIN order_items oi ON oi.order_id = o.id
            JOIN product_variants pv ON pv.id = oi.product_variant_id
            JOIN product_group pg ON pg.product_id = pv.product_id
            WHERE o.created_at >= :previous_start AND o.created_at < :start_date
              AND o.deleted_at IS NULL
              AND o.status IN ('PAID','PREPARING','SHIPPING','DELIVERED')
            GROUP BY pg.category_name
        ), refunded AS (
            SELECT pg.category_name,
                   SUM(CASE WHEN o.final_amount = 0 THEN 0
                            ELSE r.refund_amount * oi.total_price / o.final_amount END) AS refund_amount
            FROM refunds r JOIN payments pay ON pay.id = r.payment_id
            JOIN orders o ON o.id = pay.order_id
            JOIN order_items oi ON oi.order_id = o.id
            JOIN product_variants pv ON pv.id = oi.product_variant_id
            JOIN product_group pg ON pg.product_id = pv.product_id
            WHERE o.created_at >= :start_date AND o.created_at < :end_exclusive
              AND r.status = 'COMPLETED'
            GROUP BY pg.category_name
        )
        SELECT c.category_name, c.sales, c.orders,
               ROUND(c.sales / NULLIF(c.orders, 0))::bigint AS average_order_amount,
               COALESCE(ROUND(r.refund_amount), 0) AS refund_amount,
               CASE WHEN COALESCE(p.sales, 0) = 0 THEN 0
                    ELSE ROUND((c.sales-p.sales)/p.sales*100, 1) END AS growth_rate
        FROM current_period c
        LEFT JOIN previous_period p ON p.category_name = c.category_name
        LEFT JOIN refunded r ON r.category_name = c.category_name
        ORDER BY c.sales DESC, c.category_name
        """,
        parameters,
        ["category_name", "sales", "orders", "average_order_amount", "refund_amount", "growth_rate"],
    )

    products = await _query_frame(
        session,
        """
        WITH sold AS (
            SELECT pv.product_id, SUM(oi.total_price) AS sales,
                   SUM(oi.quantity) AS quantity, COUNT(DISTINCT o.id) AS orders
            FROM orders o JOIN order_items oi ON oi.order_id = o.id
            JOIN product_variants pv ON pv.id = oi.product_variant_id
            WHERE o.created_at >= :start_date AND o.created_at < :end_exclusive
              AND o.deleted_at IS NULL
              AND o.status IN ('PAID','PREPARING','SHIPPING','DELIVERED')
            GROUP BY pv.product_id
        ), stock AS (
            SELECT product_id, SUM(stock) AS current_stock
            FROM product_variants GROUP BY product_id
        ), inventory AS (
            SELECT pv.product_id,
                   SUM(CASE WHEN it.quantity_change > 0 THEN it.quantity_change ELSE 0 END) AS inbound,
                   ABS(SUM(CASE WHEN it.quantity_change < 0 THEN it.quantity_change ELSE 0 END)) AS outbound
            FROM inventory_transactions it
            JOIN product_variants pv ON pv.id = it.variant_id
            WHERE it.created_at >= :start_date AND it.created_at < :end_exclusive
            GROUP BY pv.product_id
        ), review_stats AS (
            SELECT pr.product_id, COUNT(pr.id) AS review_count,
                   ROUND(AVG(pr.rating), 2) AS average_rating,
                   COUNT(*) FILTER (WHERE rs.sentiment='POSITIVE') AS positive_reviews,
                   COUNT(*) FILTER (WHERE rs.sentiment='NEGATIVE') AS negative_reviews,
                   CASE WHEN COUNT(rs.id)=0 THEN 0 ELSE
                        ROUND(COUNT(*) FILTER (WHERE rs.sentiment='POSITIVE')::numeric/COUNT(rs.id)*100, 1)
                   END AS positive_rate
            FROM product_reviews pr LEFT JOIN review_sentiments rs ON rs.review_id=pr.id
            WHERE pr.created_at >= :start_date AND pr.created_at < :end_exclusive
              AND pr.is_visible=TRUE
            GROUP BY pr.product_id
        )
        SELECT p.id AS product_id, p.name, c.name AS category_name,
               COALESCE(s.sales, 0) AS sales, COALESCE(s.quantity, 0) AS quantity,
               COALESCE(s.orders, 0) AS orders, COALESCE(st.current_stock, 0) AS current_stock,
               COALESCE(i.inbound, 0) AS inbound, COALESCE(i.outbound, 0) AS outbound,
               COALESCE(rv.review_count, 0) AS review_count,
               COALESCE(rv.average_rating, 0) AS average_rating,
               COALESCE(rv.positive_reviews, 0) AS positive_reviews,
               COALESCE(rv.negative_reviews, 0) AS negative_reviews,
               COALESCE(rv.positive_rate, 0) AS positive_rate
        FROM products p LEFT JOIN categories c ON c.id = p.category_id
        LEFT JOIN sold s ON s.product_id = p.id
        LEFT JOIN stock st ON st.product_id = p.id
        LEFT JOIN inventory i ON i.product_id = p.id
        LEFT JOIN review_stats rv ON rv.product_id = p.id
        ORDER BY sales DESC, quantity DESC, p.id
        """,
        parameters,
        ["product_id", "name", "category_name", "sales", "quantity", "orders", "current_stock", "inbound", "outbound", "review_count", "average_rating", "positive_reviews", "negative_reviews", "positive_rate"],
    )

    reviews = await _query_frame(
        session,
        """
        SELECT p.id AS product_id, p.name,
               COUNT(pr.id) AS review_count,
               ROUND(AVG(pr.rating), 2) AS average_rating,
               COUNT(*) FILTER (WHERE rs.sentiment = 'POSITIVE') AS positive_reviews,
               COUNT(*) FILTER (WHERE rs.sentiment = 'NEGATIVE') AS negative_reviews,
               CASE WHEN COUNT(rs.id) = 0 THEN 0 ELSE
                    ROUND(COUNT(*) FILTER (WHERE rs.sentiment='POSITIVE')::numeric / COUNT(rs.id) * 100, 1)
               END AS positive_rate
        FROM products p
        LEFT JOIN product_reviews pr ON pr.product_id = p.id
             AND pr.created_at >= :start_date AND pr.created_at < :end_exclusive
             AND pr.is_visible = TRUE
        LEFT JOIN review_sentiments rs ON rs.review_id = pr.id
        GROUP BY p.id, p.name
        HAVING COUNT(pr.id) > 0
        ORDER BY review_count DESC, p.id
        """,
        parameters,
        ["product_id", "name", "review_count", "average_rating", "positive_reviews", "negative_reviews", "positive_rate"],
    )
    # 조회 종료일 다음 날을 기준으로 최근 28일 매출의 다음 30일 예측을 함께 저장합니다.
    forecast_result = await retrain_sales_forecast(session, end_date + timedelta(days=1))
    forecast = pd.DataFrame([forecast_result.model_dump()])
    return {
        "summary": summary,
        "categories": categories,
        "daily": daily,
        "products": products,
        "reviews": reviews,
        "forecast": forecast,
    }


def _csv_bytes(frame: pd.DataFrame) -> bytes:
    """Excel에서 한글이 깨지지 않도록 UTF-8 BOM을 포함합니다."""
    return frame.to_csv(index=False).encode("utf-8-sig")


def _format_export_frame(key: str, frame: pd.DataFrame) -> pd.DataFrame:
    """스키마에 정의된 열을 먼저 정렬·한글화하고, 새 데이터 열도 빠뜨리지 않는다."""
    columns = SECTION_SCHEMA[key].get("columns", {})
    ordered_columns = [column for column in columns if column in frame.columns]
    remaining_columns = [column for column in frame.columns if column not in ordered_columns]
    return frame.loc[:, ordered_columns + remaining_columns].rename(columns=columns)


def create_csv_file(
    frames: dict[str, pd.DataFrame], section: str
) -> tuple[bytes, str, str]:
    """한 항목은 CSV, 전체 항목은 여러 CSV가 들어 있는 ZIP으로 만듭니다."""
    if section != "all":
        frame = _format_export_frame(section, frames[section])
        return _csv_bytes(frame), f"{EXPORT_SCHEMA['fileNamePrefix']}_{section}.csv", "text/csv; charset=utf-8"
    output = BytesIO()
    with ZipFile(output, "w", ZIP_DEFLATED) as archive:
        for key, frame in frames.items():
            archive.writestr(
                f"{EXPORT_SCHEMA['fileNamePrefix']}_{key}.csv",
                _csv_bytes(_format_export_frame(key, frame)),
            )
    return output.getvalue(), f"{EXPORT_SCHEMA['fileNamePrefix']}_statistics_csv.zip", "application/zip"


def create_excel_file(
    frames: dict[str, pd.DataFrame], section: str
) -> tuple[bytes, str, str]:
    """전체 통계를 시트별 Excel 파일로 만듭니다."""
    selected = frames if section == "all" else {section: frames[section]}
    output = BytesIO()
    with pd.ExcelWriter(output, engine="openpyxl") as writer:
        for key, frame in selected.items():
            sheet_name = SECTION_SCHEMA[key]["sheetName"]
            _format_export_frame(key, frame).to_excel(writer, sheet_name=sheet_name, index=False)
            worksheet = writer.sheets[sheet_name]
            worksheet.freeze_panes = "A2"
            worksheet.auto_filter.ref = worksheet.dimensions
            for cells in worksheet.columns:
                width = min(max(len(str(cell.value or "")) for cell in cells) + 2, 45)
                worksheet.column_dimensions[cells[0].column_letter].width = width
    return output.getvalue(), f"{EXPORT_SCHEMA['fileNamePrefix']}_statistics.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

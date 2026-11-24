"""현재 재고와 실제 주문 이력으로 옵션별 소진 예상일과 권장 입고 수량을 계산한다."""

from math import ceil

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.common.config import settings


async def forecast_inventory_shortage(session: AsyncSession, limit: int) -> dict:
    """판매 이력이 없는 상품에는 예측 값을 만들지 않고 제외한다."""
    lookback_days = settings.INVENTORY_FORECAST_LOOKBACK_DAYS
    statement = text(
        """
        WITH sold AS (
            SELECT
                oi.product_variant_id AS variant_id,
                COALESCE(SUM(oi.quantity), 0) AS sold_quantity
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.status IN ('PAID', 'PREPARING', 'SHIPPING', 'DELIVERED')
              AND o.deleted_at IS NULL
              AND o.created_at >= CURRENT_TIMESTAMP - make_interval(days => :lookback_days)
              AND o.created_at <= CURRENT_TIMESTAMP
            GROUP BY oi.product_variant_id
        )
        SELECT
            pv.id AS variant_id,
            p.name AS product_name,
            pv.option_label,
            pv.stock,
            COALESCE(s.sold_quantity, 0) AS sold_quantity
        FROM product_variants pv
        JOIN products p ON p.id = pv.product_id
        LEFT JOIN sold s ON s.variant_id = pv.id
        WHERE pv.product_variant_status = 'ON_SALE'
        ORDER BY
            CASE WHEN COALESCE(s.sold_quantity, 0) > 0
                THEN pv.stock::numeric / s.sold_quantity
                ELSE NULL
            END ASC NULLS LAST,
            pv.id ASC
        LIMIT :limit
        """
    )
    result = await session.execute(
        statement,
        {"lookback_days": lookback_days, "limit": limit},
    )

    target_days = (
        settings.INVENTORY_FORECAST_LEAD_DAYS
        + settings.INVENTORY_FORECAST_SAFETY_DAYS
    )
    items = []
    for row in result.mappings().all():
        item = dict(row)
        daily_sales = float(item["sold_quantity"] or 0) / lookback_days
        stock = int(item["stock"] or 0)
        days_until_stockout = round(stock / daily_sales, 1) if daily_sales else None
        recommended_inbound = max(0, ceil(daily_sales * target_days - stock))
        items.append(
            {
                "variantId": int(item["variant_id"]),
                "productName": str(item["product_name"]),
                "optionLabel": str(item["option_label"]),
                "stock": stock,
                "soldQuantity": int(item["sold_quantity"] or 0),
                "averageDailySales": round(daily_sales, 2),
                "daysUntilStockout": days_until_stockout,
                "recommendedInboundQuantity": recommended_inbound,
            }
        )
    return {
        "lookbackDays": lookback_days,
        "leadDays": settings.INVENTORY_FORECAST_LEAD_DAYS,
        "safetyDays": settings.INVENTORY_FORECAST_SAFETY_DAYS,
        "items": items,
    }

"""PostgreSQL 주문 이력을 이용한 가벼운 매출 예측 재학습 서비스입니다."""

from datetime import date, datetime, timedelta

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.sales.schemas import SalesForecastResponse


async def retrain_sales_forecast(
    session: AsyncSession, forecast_date: date
) -> SalesForecastResponse:
    """최근 28일 일매출의 평균·추세를 다시 계산해 다음 30일을 예측한다."""
    start_date = forecast_date - timedelta(days=28)
    statement = text(
        """
        SELECT created_at::date AS sale_date, COALESCE(SUM(final_amount), 0) AS sales
        FROM orders
        WHERE created_at::date >= :start_date
          AND created_at::date < :forecast_date
          AND status IN ('PAID', 'PREPARING', 'SHIPPING', 'DELIVERED')
        GROUP BY created_at::date
        ORDER BY sale_date
        """
    )
    result = await session.execute(
        statement, {"start_date": start_date, "forecast_date": forecast_date}
    )
    values = [int(row.sales or 0) for row in result.mappings().all()]
    training_days = len(values)
    average_daily_sales = sum(values) / training_days if training_days else 0
    midpoint = max(1, training_days // 2)
    previous_average = sum(values[:midpoint]) / midpoint if training_days else 0
    recent_count = max(1, training_days - midpoint)
    recent_average = sum(values[midpoint:]) / recent_count if training_days else 0
    growth_rate = (
        round((recent_average - previous_average) / previous_average * 100, 1)
        if previous_average
        else 0.0
    )
    predicted_sales = max(0, round(average_daily_sales * 30 * (1 + growth_rate / 100)))
    insight = (
        "최근 결제 완료 주문 데이터가 없어 예측값을 0원으로 표시합니다."
        if not training_days
        else f"최근 {training_days}일 결제 완료 주문을 다시 학습했습니다. 직전 구간 대비 일매출 추세는 {growth_rate:+.1f}%입니다."
    )
    return SalesForecastResponse(
        predicted_sales=predicted_sales,
        growth_rate=growth_rate,
        forecast_date=forecast_date,
        trained_at=datetime.now(),
        training_days=training_days,
        insight=insight,
    )

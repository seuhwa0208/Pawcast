"""PostgreSQL 주문 이력을 이용한 가벼운 매출 예측 재학습 서비스입니다."""

from datetime import date, datetime, timedelta

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.sales.schemas import SalesForecastResponse
from app.sales.schemas import (
    CategorySalesResponse,
    DashboardForecastResponse,
    ForecastProductResponse,
    SalesDashboardResponse,
    SalesSummaryResponse,
    SalesTrendResponse,
)


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


async def create_sales_dashboard(
    session: AsyncSession,
    start_date: date,
    end_date: date,
    unit: str,
) -> SalesDashboardResponse:
    """CSV 다운로드와 동일한 DB 집계 결과를 관리자 화면 응답으로 변환합니다."""
    from app.sales.export_service import load_sales_export_frames

    frames = await load_sales_export_frames(session, start_date, end_date)
    summary_frame = frames["summary"]
    summary_row = summary_frame.iloc[0].to_dict() if not summary_frame.empty else {}

    daily = frames["daily"].copy()
    if not daily.empty:
        daily["sale_date"] = daily["sale_date"].astype("datetime64[ns]")
        if unit == "day":
            daily["period"] = daily["sale_date"].dt.strftime("%m-%d")
        elif unit == "quarter":
            daily["period"] = daily["sale_date"].dt.year.astype(str) + " Q" + daily["sale_date"].dt.quarter.astype(str)
        elif unit == "year":
            daily["period"] = daily["sale_date"].dt.strftime("%Y")
        else:
            daily["period"] = daily["sale_date"].dt.strftime("%Y-%m")
        trend_frame = daily.groupby("period", sort=True, as_index=False).agg(
            sales=("sales", "sum"),
            orders=("orders", "sum"),
        )
    else:
        trend_frame = daily

    category_items = [
        CategorySalesResponse(
            key=str(row["category_name"]),
            name=str(row["category_name"]),
            sales=int(row["sales"] or 0),
            orders=int(row["orders"] or 0),
            refund=int(row["refund_amount"] or 0),
            growth=float(row["growth_rate"] or 0),
        )
        for row in frames["categories"].to_dict("records")
    ]

    forecast_row = frames["forecast"].iloc[0].to_dict()
    top_products = [
        ForecastProductResponse(name=str(row["name"]), growthRate=0)
        for row in frames["products"].head(3).to_dict("records")
    ]

    return SalesDashboardResponse(
        summary=SalesSummaryResponse(
            totalSales=int(summary_row.get("total_sales", 0) or 0),
            totalOrders=int(summary_row.get("total_orders", 0) or 0),
            averageOrderAmount=int(summary_row.get("average_order_amount", 0) or 0),
            refundAmount=int(summary_row.get("refund_amount", 0) or 0),
        ),
        trend=SalesTrendResponse(
            labels=trend_frame["period"].astype(str).tolist() if not trend_frame.empty else [],
            sales=[int(value) for value in trend_frame["sales"].tolist()] if not trend_frame.empty else [],
            orders=[int(value) for value in trend_frame["orders"].tolist()] if not trend_frame.empty else [],
        ),
        categories=category_items,
        forecast=DashboardForecastResponse(
            predictedSales=int(forecast_row.get("predicted_sales", 0) or 0),
            growthRate=float(forecast_row.get("growth_rate", 0) or 0),
            insight=str(forecast_row.get("insight", "예측 데이터가 없습니다.")),
            topProducts=top_products,
        ),
    )

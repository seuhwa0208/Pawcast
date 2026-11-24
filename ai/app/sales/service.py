"""PostgreSQL 주문 이력을 이용한 가벼운 매출 예측 재학습 서비스입니다."""

from datetime import date, datetime, timedelta

import pandas as pd
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
    """최근 28일 학습으로 기준일 다음 달 전체의 일별 매출을 예측한다."""
    # forecast_date는 예측 기준일(당일 포함)로 정의합니다. 이 날짜까지의
    # 28일을 학습에 쓰기 위해 SQL 종료 조건은 다음 날 미만으로 둡니다.
    start_date = forecast_date - timedelta(days=27)
    training_end_exclusive = forecast_date + timedelta(days=1)
    statement = text(
        """
        SELECT created_at::date AS sale_date, COALESCE(SUM(final_amount), 0) AS sales
        FROM orders
        WHERE created_at::date >= :start_date
          AND created_at::date < :training_end_exclusive
          AND deleted_at IS NULL
          AND status IN ('PAID', 'PREPARING', 'SHIPPING', 'DELIVERED')
        GROUP BY created_at::date
        ORDER BY sale_date
        """
    )
    result = await session.execute(
        statement,
        {
            "start_date": start_date,
            "training_end_exclusive": training_end_exclusive,
        },
    )
    rows = result.mappings().all()

    # 매출이 발생하지 않은 날짜도 0원으로 포함해야 28일 평균이 실제 일평균이 됩니다.
    # 기존에는 주문이 있었던 날짜만 평균 내어 학습 기간·증감률이 과장될 수 있었습니다.
    sales_by_date = {
        row.sale_date: int(row.sales or 0)
        for row in rows
    }
    training_days = (training_end_exclusive - start_date).days
    daily_sales = [
        sales_by_date.get(start_date + timedelta(days=offset), 0)
        for offset in range(training_days)
    ]
    has_sales_data = any(value > 0 for value in daily_sales)

    midpoint = max(1, training_days // 2)
    previous_values = daily_sales[:midpoint]
    recent_values = daily_sales[midpoint:]
    previous_average = (
        sum(previous_values) / len(previous_values)
        if previous_values
        else 0
    )
    recent_average = (
        sum(recent_values) / len(recent_values)
        if recent_values
        else 0
    )
    growth_rate = (
        round((recent_average - previous_average) / previous_average * 100, 1)
        if previous_average > 0
        else 0.0
    )
    average_daily_sales = (
        sum(daily_sales) / training_days
        if training_days
        else 0
    )
    # 예측 대상은 임의 30일이 아니라 기준일 다음 달의 실제 달력 일수입니다.
    target_month_start = (
        forecast_date.replace(day=1) + timedelta(days=32)
    ).replace(day=1)
    following_month_start = (
        target_month_start.replace(day=1) + timedelta(days=32)
    ).replace(day=1)
    target_month_end = following_month_start - timedelta(days=1)
    forecast_days = (target_month_end - target_month_start).days + 1

    # 일평균 기준선에 최근 14일 대 직전 14일 추세를 적용한 일별 예측을
    # 대상 월의 매일에 생성하고, 그 달에 속한 값만 합산해 월 예측액을 냅니다.
    forecast_daily_sales = [
        max(0, round(average_daily_sales * (1 + growth_rate / 100)))
        for _ in range(forecast_days)
    ]
    predicted_sales = sum(forecast_daily_sales)
    growth_comparison = (
        "최근 14일 일평균 매출과 그 직전 14일 일평균 매출 비교: "
        "(최근 14일 평균 - 직전 14일 평균) / 직전 14일 평균 × 100. "
        "직전 14일 평균이 0원이면 증감률은 0%로 처리합니다."
    )

    product_result = await session.execute(
        text(
            """
            SELECT p.name,
                   COALESCE(SUM(oi.total_price), 0) AS sales,
                   COALESCE(SUM(oi.quantity), 0) AS quantity
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN product_variants pv ON pv.id = oi.product_variant_id
            JOIN products p ON p.id = pv.product_id
            WHERE o.created_at::date >= :start_date
              AND o.created_at::date < :training_end_exclusive
              AND o.deleted_at IS NULL
              AND o.status IN ('PAID', 'PREPARING', 'SHIPPING', 'DELIVERED')
            GROUP BY p.id, p.name
            ORDER BY sales DESC, quantity DESC, p.id
            LIMIT 3
            """
        ),
        {
            "start_date": start_date,
            "training_end_exclusive": training_end_exclusive,
        },
    )
    top_products = [
        ForecastProductResponse(
            name=str(row.name),
            sales=int(row.sales or 0),
            quantity=int(row.quantity or 0),
            growthRate=0,
        )
        for row in product_result.mappings().all()
    ]

    insight = (
        "최근 28일간 결제 완료 매출 데이터가 없어 예측값을 0원으로 표시합니다."
        if not has_sales_data
        else (
            f"최근 {training_days}일의 일별 매출을 반영해 "
            f"{target_month_start:%Y년 %m월} {forecast_days}일 합계를 예측했습니다. "
            f"직전 14일 대비 최근 14일 일매출 추세는 {growth_rate:+.1f}%입니다."
        )
    )
    return SalesForecastResponse(
        predicted_sales=predicted_sales,
        growth_rate=growth_rate,
        forecast_date=forecast_date,
        trained_at=datetime.now(),
        training_days=training_days,
        forecastBaseDate=forecast_date,
        targetMonth=target_month_start.strftime("%Y-%m"),
        forecastStartDate=target_month_start,
        forecastEndDate=target_month_end,
        forecastDays=forecast_days,
        isCumulative=False,
        growthComparison=growth_comparison,
        previousDailyAverage=round(previous_average, 2),
        recentDailyAverage=round(recent_average, 2),
        insight=insight,
        top_products=top_products,
    )


def _quarter_period_labels(start_date: date, end_date: date) -> list[str]:
    """선택한 날짜 범위와 겹치는 분기를 빠짐없이 시간순으로 반환합니다."""
    quarter_month = ((start_date.month - 1) // 3) * 3 + 1
    current = date(start_date.year, quarter_month, 1)
    labels = []

    while current <= end_date:
        quarter = ((current.month - 1) // 3) + 1
        labels.append(f"{current.year} Q{quarter}")
        next_month = current.month + 3
        current = date(
            current.year + (next_month - 1) // 12,
            ((next_month - 1) % 12) + 1,
            1,
        )

    return labels


async def create_sales_dashboard(
    session: AsyncSession,
    start_date: date,
    end_date: date,
    unit: str,
    forecast_date: date | None = None,
) -> SalesDashboardResponse:
    """CSV 다운로드와 동일한 DB 집계 결과를 관리자 화면 응답으로 변환합니다."""
    from app.sales.export_service import load_sales_export_frames

    # 선택한 분석 기간과 예측 학습 기간은 별개입니다. Q4 조회가 미래 기간을
    # 포함하더라도 예측은 실제 오늘 이후의 최근 학습 구간을 기준으로 계산합니다.
    effective_forecast_date = forecast_date or date.today()
    frames = await load_sales_export_frames(
        session,
        start_date,
        end_date,
        forecast_date=effective_forecast_date,
    )
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
        trend_frame = pd.DataFrame(
            columns=["period", "sales", "orders"],
        )

    if unit == "quarter":
        # 빈 Q4도 0원 분기로 응답해 프론트 차트와 KPI에서 누락되지 않게 합니다.
        expected_periods = pd.DataFrame(
            {"period": _quarter_period_labels(start_date, end_date)},
        )
        trend_frame = expected_periods.merge(
            trend_frame,
            on="period",
            how="left",
        )
        trend_frame[["sales", "orders"]] = (
            trend_frame[["sales", "orders"]]
            .fillna(0)
            .astype(int)
        )

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
        ForecastProductResponse(
            name=str(row["name"]),
            sales=int(row.get("sales", 0) or 0),
            quantity=int(row.get("quantity", 0) or 0),
            growthRate=0,
        )
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
            forecastBaseDate=forecast_row.get("forecastBaseDate"),
            targetMonth=str(forecast_row.get("targetMonth", "")),
            forecastStartDate=forecast_row.get("forecastStartDate"),
            forecastEndDate=forecast_row.get("forecastEndDate"),
            forecastDays=int(forecast_row.get("forecastDays", 1) or 1),
            isCumulative=False,
            growthComparison=str(forecast_row.get("growthComparison", "")),
            previousDailyAverage=float(forecast_row.get("previousDailyAverage", 0) or 0),
            recentDailyAverage=float(forecast_row.get("recentDailyAverage", 0) or 0),
            insight=str(forecast_row.get("insight", "예측 데이터가 없습니다.")),
            topProducts=top_products,
        ),
    )

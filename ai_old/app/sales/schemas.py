"""매출 분석 요청·응답 DTO를 정의하는 모듈입니다."""

from datetime import date, datetime

from pydantic import BaseModel, Field


class SalesSummaryResponse(BaseModel):
    totalSales: int = 0
    totalOrders: int = 0
    averageOrderAmount: int = 0
    refundAmount: int = 0


class SalesTrendResponse(BaseModel):
    labels: list[str] = Field(default_factory=list)
    sales: list[int] = Field(default_factory=list)
    orders: list[int] = Field(default_factory=list)


class CategorySalesResponse(BaseModel):
    key: str
    name: str
    sales: int = 0
    orders: int = 0
    refund: int = 0
    growth: float = 0


class ForecastProductResponse(BaseModel):
    name: str
    growthRate: float = 0


class DashboardForecastResponse(BaseModel):
    predictedSales: int = 0
    growthRate: float = 0
    insight: str
    topProducts: list[ForecastProductResponse] = Field(default_factory=list)


class SalesDashboardResponse(BaseModel):
    summary: SalesSummaryResponse
    trend: SalesTrendResponse
    categories: list[CategorySalesResponse]
    forecast: DashboardForecastResponse


class SalesForecastResponse(BaseModel):
    """관리자 화면이 바로 표시할 수 있는 재학습 결과입니다."""

    predicted_sales: int = Field(ge=0)
    growth_rate: float
    forecast_date: date
    trained_at: datetime
    training_days: int = Field(ge=0)
    insight: str

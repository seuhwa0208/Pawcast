"""매출 분석 요청·응답 DTO를 정의하는 모듈입니다."""

from datetime import date, datetime

from pydantic import BaseModel, Field


class SalesForecastResponse(BaseModel):
    """관리자 화면이 바로 표시할 수 있는 재학습 결과입니다."""

    predicted_sales: int = Field(ge=0)
    growth_rate: float
    forecast_date: date
    trained_at: datetime
    training_days: int = Field(ge=0)
    insight: str

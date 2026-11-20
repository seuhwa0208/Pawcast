"""매출 분석 FastAPI 엔드포인트를 정의하는 모듈입니다."""

from datetime import date

from fastapi import APIRouter, Depends, Header, HTTPException, Query, status
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.common.config import settings
from app.common.database import get_db_session
from app.sales.schemas import SalesForecastResponse
from app.sales.export_service import create_csv_file, create_excel_file, load_sales_export_frames
from app.sales.service import retrain_sales_forecast


router = APIRouter(prefix="/api/sales", tags=["매출 분석"])


@router.post("/retrain", response_model=SalesForecastResponse)
async def retrain(
    forecast_date: date,
    session: AsyncSession = Depends(get_db_session),
    internal_api_key: str | None = Header(default=None),
) -> SalesForecastResponse:
    """관리자 요청으로 판매 예측 베이스라인을 다시 계산한다."""
    if settings.INTERNAL_API_KEY and internal_api_key != settings.INTERNAL_API_KEY:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid internal API key")
    return await retrain_sales_forecast(session, forecast_date)


@router.get("/export")
async def export_sales_statistics(
    start_date: date,
    end_date: date,
    file_format: str = Query(default="xlsx", alias="format", pattern="^(csv|xlsx)$"),
    section: str = Query(default="all", pattern="^(all|summary|categories|daily|products|reviews|forecast)$"),
    session: AsyncSession = Depends(get_db_session),
    internal_api_key: str | None = Header(default=None),
) -> Response:
    """기간별 매출 통계를 CSV·ZIP 또는 Excel 파일로 다운로드합니다."""
    if settings.INTERNAL_API_KEY and internal_api_key != settings.INTERNAL_API_KEY:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid internal API key")
    if start_date > end_date:
        raise HTTPException(status_code=400, detail="start_date must be before or equal to end_date")

    frames = await load_sales_export_frames(session, start_date, end_date)
    if file_format == "csv":
        content, filename, media_type = create_csv_file(frames, section)
    else:
        content, filename, media_type = create_excel_file(frames, section)
    return Response(
        content=content,
        media_type=media_type,
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )

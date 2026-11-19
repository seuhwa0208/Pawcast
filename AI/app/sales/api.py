"""매출 분석 FastAPI 엔드포인트를 정의하는 모듈입니다."""

from fastapi import APIRouter


router = APIRouter(prefix="/api/sales", tags=["매출 분석"])

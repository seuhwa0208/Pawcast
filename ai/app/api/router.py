"""PAWCAST AI 도메인의 API 라우터를 한곳에서 조합합니다."""

from fastapi import APIRouter

from app.recommendation.api import router as recommendation_router
from app.review.api import router as review_router
from app.sales.api import router as sales_router


api_router = APIRouter()
api_router.include_router(review_router)
api_router.include_router(sales_router)
api_router.include_router(recommendation_router)

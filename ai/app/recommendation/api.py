"""Spring Boot가 호출하는 상품 추천 FastAPI 엔드포인트입니다."""

from fastapi import APIRouter

from app.recommendation.schemas import (
    MemberRecommendationResponse,
    RecommendationRequest,
)
from app.recommendation.service import recommend_products


router = APIRouter(prefix="/api/recommendations", tags=["상품 추천"])


@router.post("", response_model=MemberRecommendationResponse)
def create_recommendations(
    request: RecommendationRequest,
) -> MemberRecommendationResponse:
    """생일로 생애주기를 계산하고 조건에 맞는 상품을 최대 5개 반환합니다."""

    return recommend_products(request)

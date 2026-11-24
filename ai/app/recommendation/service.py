"""후보 생성, 생애주기 필터, 랭킹을 연결하는 추천 파이프라인입니다."""

from app.recommendation.candidate import create_candidates
from app.recommendation.ranking import rank_candidates
from app.recommendation.rules import (
    determine_life_stage,
    passes_recommendation_rules,
)
from app.recommendation.schemas import (
    MemberRecommendationResponse,
    RecommendationRequest,
)


def recommend_products(
    request: RecommendationRequest,
) -> MemberRecommendationResponse:
    life_stage, age_months = determine_life_stage(request.pet)
    candidates = create_candidates(request)
    eligible_products = [
        product
        for product in candidates
        if passes_recommendation_rules(product, request.pet, life_stage)
    ]
    items = rank_candidates(eligible_products, request, life_stage)

    return MemberRecommendationResponse(
        member_id=request.member_id,
        pet_id=request.pet.pet_id,
        life_stage=life_stage,
        age_months=age_months,
        items=items,
    )

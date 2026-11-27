"""추천 후보의 점수를 계산하고 상위 상품 순서를 결정합니다."""

from math import log1p

from app.recommendation.schemas import (
    LifeStage,
    RecommendationCandidate,
    RecommendationItem,
    RecommendationRequest,
)
from app.recommendation.rules import parse_product_life_stages


def score_candidate(
    product: RecommendationCandidate,
    request: RecommendationRequest,
    life_stage: LifeStage,
) -> tuple[float, list[str]]:
    score = 0.0
    reasons: list[str] = []
    product_stages = parse_product_life_stages(product.life_stage)

    if life_stage in product_stages:
        score += 40
        reasons.append(f"{life_stage.value} 생애주기 맞춤 상품")
    elif LifeStage.ALL_AGES in product_stages:
        score += 30
        reasons.append("전연령 급여 가능 상품")
    else:
        score += 10

    if product.category_id in request.preferred_category_ids:
        score += 15
        reasons.append("선호 카테고리")
    if product.product_id in request.wish_product_ids:
        score += 10
        reasons.append("찜한 상품")
    if product.product_id in request.peer_product_ids:
        score += 8
        reasons.append("비슷한 회원이 선택한 상품")

    score += float(product.average_rating) * 4
    score += min(log1p(product.review_count) * 1.5, 8)
    score += min(log1p(product.sales_count), 7)
    if float(product.average_rating) >= 4.5:
        reasons.append("높은 구매자 평점")

    if not reasons:
        reasons.append("반려동물 조건에 맞는 판매 상품")
    return round(score, 2), reasons


def rank_candidates(
    products: list[RecommendationCandidate],
    request: RecommendationRequest,
    life_stage: LifeStage,
) -> list[RecommendationItem]:
    scored = [
        (product, *score_candidate(product, request, life_stage))
        for product in products
    ]
    scored.sort(
        key=lambda value: (
            value[1],
            float(value[0].average_rating),
            value[0].review_count,
            -value[0].product_id,
        ),
        reverse=True,
    )

    items: list[RecommendationItem] = []
    for rank, (product, score, reasons) in enumerate(
        scored[: request.limit],
        start=1,
    ):
        items.append(
            RecommendationItem(
                rank=rank,
                product_id=product.product_id,
                variant_id=product.variant_id,
                name=product.name,
                brand=product.brand,
                category_name=product.category_name,
                image_path=product.image_path,
                sale_price=product.sale_price,
                average_rating=product.average_rating,
                review_count=product.review_count,
                recommendation_score=score,
                recommendation_reasons=reasons,
            )
        )
    return items

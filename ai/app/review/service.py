"""DB 리뷰 조회, 모델 분석, 저장과 베스트 리뷰 응답을 연결합니다."""

from datetime import datetime
from decimal import Decimal

from fastapi import HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from starlette.concurrency import run_in_threadpool

from app.common.config import settings
from app.review.repository import (
    find_best_review_products,
    find_pending_visible_review_ids,
    find_recent_reviews_needing_sentiment,
    find_ranked_product_reviews,
    find_visible_review,
    save_sentiment,
)
from app.review.schemas import (
    BestReviewProductResponse,
    BestReviewResponse,
    RankedReviewResponse,
    StoredSentimentResponse,
)
from app.review.sentiment_service import get_sentiment_analyzer


SENTIMENT_RESPONSE_MAP = {
    "긍정": "POSITIVE",
    "부정": "NEGATIVE",
}


async def analyze_pending_reviews(session: AsyncSession, limit: int) -> dict:
    """기존 공개 리뷰 중 분석 이력이 없는 항목을 제한 수량만 보정한다."""
    review_ids = await find_pending_visible_review_ids(session, limit)
    analyzed = 0
    for review_id in review_ids:
        await analyze_and_store_review(session, review_id)
        analyzed += 1
    return {"requestedCount": len(review_ids), "analyzedCount": analyzed}


async def analyze_recent_reviews(
    session: AsyncSession,
    days: int,
    limit: int,
) -> dict:
    """최근 기간의 미분석·레거시 감정값을 모델로 분석해 저장합니다."""
    review_ids = await find_recent_reviews_needing_sentiment(
        session,
        days,
        limit,
    )
    analyzed = 0

    # 분석 결과는 리뷰마다 저장·커밋합니다. 중간에 한 건이 실패해도 앞선
    # 성공 결과를 잃지 않으며, 재요청 시 남은 리뷰부터 이어서 처리할 수 있습니다.
    for review_id in review_ids:
        await analyze_and_store_review(session, review_id)
        analyzed += 1

    remaining = await find_recent_reviews_needing_sentiment(
        session,
        days,
        1,
    )
    return {
        "requestedCount": len(review_ids),
        "analyzedCount": analyzed,
        "remainingCount": len(remaining),
        "periodDays": days,
    }


async def analyze_and_store_review(
    session: AsyncSession,
    review_id: int,
) -> StoredSentimentResponse:
    review = await find_visible_review(session, review_id)
    if review is None:
        raise HTTPException(status_code=404, detail="감정 분석할 공개 리뷰가 없습니다.")

    analyzer = get_sentiment_analyzer()
    result = await run_in_threadpool(analyzer.predict, review.content)
    sentiment = SENTIMENT_RESPONSE_MAP.get(result["sentiment"])
    if sentiment is None:
        raise HTTPException(status_code=500, detail="지원하지 않는 감정 분석 결과입니다.")

    confidence = Decimal(str(round(float(result["confidence"]), 6)))
    await save_sentiment(
        session,
        review.review_id,
        sentiment,
        confidence,
        analyzer.model_name,
    )
    await session.commit()

    return StoredSentimentResponse(
        reviewId=review.review_id,
        sentiment=sentiment,
        confidence=confidence,
        positivePercentage=round(float(result["positive_score"]) * 100, 2),
        modelName=analyzer.model_name,
        analyzedAt=datetime.now().astimezone(),
    )


async def get_best_review_products(
    session: AsyncSession,
    limit: int,
) -> list[BestReviewProductResponse]:
    rows = await find_best_review_products(session, limit)
    return [
        BestReviewProductResponse(
            id=row["id"],
            name=row["name"],
            brand=row["brand"],
            productType=row["product_type"],
            categoryName=row["category_name"],
            imagePath=row["image_path"],
            variantId=row["variant_id"],
            price=row["price"],
            salesCount=row["sales_count"],
            wishCount=row["wish_count"],
            reviewCount=row["review_count"],
            averageRating=row["average_rating"],
            soldOut=row["sold_out"],
            bestReview=BestReviewResponse(
                id=row["review_id"],
                rating=row["best_review_rating"],
                content=row["best_review_content"],
                writerId=row["writer_id"] or "unknown",
                createdAt=row["best_review_created_at"],
                positivePercentage=float(row["positive_percentage"] or 0),
            ),
        )
        for row in rows
    ]


async def get_ranked_product_reviews(
    session: AsyncSession,
    product_id: int,
) -> list[RankedReviewResponse]:
    rows = await find_ranked_product_reviews(session, product_id)
    return [
        RankedReviewResponse(
            id=row["id"],
            rating=row["rating"],
            content=row["content"],
            writerName=row["writer_name"],
            createdAt=row["created_at"],
            sentiment=row["sentiment"],
            positivePercentage=float(row["positive_percentage"] or 0),
        )
        for row in rows
    ]

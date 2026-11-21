from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from starlette.concurrency import run_in_threadpool

from app.review.profanity_service import contains_prohibited_word
from app.common.database import get_db_session
from app.review.schemas import (
    BestReviewProductResponse,
    ReviewRequest,
    SentimentResponse,
    StoredSentimentResponse,
)
from app.review.service import analyze_and_store_review, get_best_review_products
from app.review.sentiment_service import get_sentiment_analyzer

router = APIRouter(prefix="/api/reviews", tags=["한국어 리뷰 감정 분석"])

SENTIMENT_RESPONSE_MAP = {
    "긍정": "POSITIVE",
    "부정": "NEGATIVE",
}


@router.get(
    "/best-products",
    response_model=list[BestReviewProductResponse],
    response_model_by_alias=True,
)
async def best_review_products(
    limit: int = Query(default=5, ge=1, le=20),
    session: AsyncSession = Depends(get_db_session),
) -> list[BestReviewProductResponse]:
    """평균 평점 상위 상품별 베스트 리뷰를 반환합니다."""
    return await get_best_review_products(session, limit)


@router.post(
    "/{review_id}/analyze",
    response_model=StoredSentimentResponse,
    response_model_by_alias=True,
)
async def analyze_saved_review(
    review_id: int,
    session: AsyncSession = Depends(get_db_session),
) -> StoredSentimentResponse:
    """Spring이 저장한 리뷰를 Python이 DB에서 읽어 분석 결과를 직접 저장합니다."""
    return await analyze_and_store_review(session, review_id)


@router.post(
    "/analyze",
    response_model=SentimentResponse,
    response_model_by_alias=True,
)
async def analyze_review(request: ReviewRequest) -> SentimentResponse:
    """임시 Spring Boot 연동 API: 계약 확정 후 경로와 응답을 고정합니다."""
    text = request.content

    # [임시 백엔드 연결 API] Java ReviewCreateRequest.content를 그대로 받습니다.
    # 프론트엔드는 이 AI API를 직접 호출하지 않고 Spring Boot를 통해 접근합니다.
    # 욕설은 감정 분석과 DB 저장 전에 차단하며, Java가 식별할 오류 코드를 함께 보냅니다.
    if contains_prohibited_word(text):
        raise HTTPException(
            status_code=400,
            detail={
                "code": "PROHIBITED_WORD",
                "message": "욕설이 포함된 리뷰는 등록할 수 없습니다.",
            },
        )

    # 모델 추론은 CPU/GPU 연산이므로 이벤트 루프를 막지 않도록 스레드 풀에서 실행합니다.
    analyzer = get_sentiment_analyzer()
    result = await run_in_threadpool(analyzer.predict, text)

    # [임시 백엔드 응답 계약] Java ReviewSentiment 필드와 같은 JSON을 반환합니다.
    # Spring AI Client DTO가 확정되면 해당 DTO와 함께 최종 점검해야 합니다.
    try:
        sentiment = SENTIMENT_RESPONSE_MAP[result["sentiment"]]
    except KeyError as error:
        raise HTTPException(
            status_code=500,
            detail={
                "code": "INVALID_MODEL_LABEL",
                "message": "감성 모델이 지원하지 않는 결과를 반환했습니다.",
            },
        ) from error

    return SentimentResponse(
        sentiment=sentiment,
        confidence=result["confidence"],
        model_name=analyzer.model_name,
        analyzed_at=datetime.now(),
    )

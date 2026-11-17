from datetime import datetime

from fastapi import APIRouter, HTTPException
from starlette.concurrency import run_in_threadpool

from app.schemas.review_schema import ReviewRequest, SentimentResponse
from app.services.profanity_service import contains_prohibited_word
from app.services.sentiment_service import get_sentiment_analyzer

router = APIRouter(prefix="/api/reviews", tags=["한국어 리뷰 감정 분석"])


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
    return SentimentResponse(
        sentiment=result["sentiment"],
        confidence=result["confidence"],
        model_name=analyzer.model_name,
        analyzed_at=datetime.now(),
    )

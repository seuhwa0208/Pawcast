from datetime import datetime
from decimal import Decimal
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class ReviewRequest(BaseModel):
    """Spring Boot가 감정 분석 API에 전달하는 요청 형식입니다."""

    model_config = ConfigDict(
        str_strip_whitespace=True,
    )

    content: str = Field(
        min_length=2,
        max_length=5000,
        description="한국어 상품 리뷰 내용",
    )


class SentimentResponse(BaseModel):
    """Spring Boot의 ReviewSentiment 엔티티에 저장할 분석 결과입니다."""

    model_config = ConfigDict(
        populate_by_name=True,
    )

    sentiment: Literal["POSITIVE", "NEGATIVE"] = Field(
        description="감정 분석 결과",
    )

    confidence: float = Field(
        ge=0.0,
        le=1.0,
        description="예측 결과의 신뢰도",
    )

    model_name: str = Field(
        alias="modelName",
        description="감정 분석에 사용된 모델명",
    )

    analyzed_at: datetime = Field(
        alias="analyzedAt",
        description="분석 완료 시각",
    )


class StoredSentimentResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    review_id: int = Field(alias="reviewId", gt=0)
    sentiment: Literal["POSITIVE", "NEGATIVE"]
    confidence: Decimal = Field(ge=0, le=1)
    positive_percentage: float = Field(alias="positivePercentage", ge=0, le=100)
    model_name: str = Field(alias="modelName")
    analyzed_at: datetime = Field(alias="analyzedAt")


class BestReviewResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: int
    rating: int = Field(ge=1, le=5)
    content: str
    writer_id: str = Field(alias="writerId")
    created_at: datetime = Field(alias="createdAt")
    positive_percentage: float = Field(alias="positivePercentage", ge=0, le=100)


class RankedReviewResponse(BaseModel):
    """상품 상세에서 AI 기준으로 정렬해 표시할 리뷰입니다."""

    model_config = ConfigDict(populate_by_name=True)

    id: int
    rating: int = Field(ge=1, le=5)
    content: str
    writer_name: str = Field(alias="writerName")
    created_at: datetime = Field(alias="createdAt")
    sentiment: Literal["POSITIVE", "NEGATIVE"] | None = None
    positive_percentage: float = Field(alias="positivePercentage", ge=0, le=100)


class BestReviewProductResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: int
    name: str
    brand: str | None = None
    product_type: str | None = Field(default=None, alias="productType")
    category_name: str | None = Field(default=None, alias="categoryName")
    image_path: str | None = Field(default=None, alias="imagePath")
    variant_id: int = Field(alias="variantId")
    price: Decimal = Field(ge=0)
    sales_count: int = Field(alias="salesCount", ge=0)
    wish_count: int = Field(alias="wishCount", ge=0)
    review_count: int = Field(alias="reviewCount", ge=0)
    average_rating: Decimal = Field(alias="averageRating", ge=0, le=5)
    sold_out: bool = Field(alias="soldOut")
    best_review: BestReviewResponse = Field(alias="bestReview")


class SentimentProductSummary(BaseModel):
    """감성 분석이 완료된 리뷰만 기준으로 한 상품별 부정 리뷰 현황."""

    model_config = ConfigDict(populate_by_name=True)

    product_id: int = Field(alias="productId")
    product_name: str = Field(alias="productName")
    negative_count: int = Field(alias="negativeCount", ge=0)
    analyzed_count: int = Field(alias="analyzedCount", ge=0)
    negative_rate: float = Field(alias="negativeRate", ge=0, le=100)


class SentimentSummaryResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    analyzed_count: int = Field(alias="analyzedCount", ge=0)
    positive_count: int = Field(alias="positiveCount", ge=0)
    negative_count: int = Field(alias="negativeCount", ge=0)
    positive_rate: float = Field(alias="positiveRate", ge=0, le=100)
    negative_rate: float = Field(alias="negativeRate", ge=0, le=100)
    negative_products: list[SentimentProductSummary] = Field(alias="negativeProducts")
    period_days: int = Field(default=28, alias="periodDays", ge=1)

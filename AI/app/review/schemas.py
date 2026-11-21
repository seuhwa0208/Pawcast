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


class BestReviewResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: int
    rating: int = Field(ge=1, le=5)
    content: str
    writer_name: str = Field(alias="writerName")
    created_at: datetime = Field(alias="createdAt")
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

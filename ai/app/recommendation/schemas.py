"""Spring Boot와 FastAPI 사이의 추천 요청·응답 계약입니다."""

from datetime import date
from decimal import Decimal
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


def to_camel(value: str) -> str:
    first, *rest = value.split("_")
    return first + "".join(word.capitalize() for word in rest)


class ApiModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True, str_strip_whitespace=True)


class PetType(str, Enum):
    DOG = "DOG"
    CAT = "CAT"


class LifeStage(str, Enum):
    PUPPY = "PUPPY"
    JUNIOR = "JUNIOR"
    ADULT = "ADULT"
    SENIOR = "SENIOR"
    ALL_AGES = "ALL_AGES"


class PetProfile(ApiModel):
    pet_id: int
    pet_type: PetType
    birthday: date
    breed_name: str | None = None
    weight_kg: Decimal | None = None
    allergies: list[str] = Field(default_factory=list)


class RecommendationCandidate(ApiModel):
    product_id: int
    variant_id: int
    name: str
    brand: str | None = None
    category_id: int
    category_name: str | None = None
    image_path: str | None = None
    sale_price: Decimal
    average_rating: Decimal = Decimal("0")
    review_count: int = 0
    sales_count: int = 0
    sold_out: bool = False
    pet_type: str
    main_ingredient: str | None = None
    life_stage: str = "ALL_AGES"


class RecommendationRequest(ApiModel):
    member_id: int
    pet: PetProfile
    purchased_product_ids: list[int] = Field(default_factory=list)
    preferred_category_ids: list[int] = Field(default_factory=list)
    wish_product_ids: list[int] = Field(default_factory=list)
    peer_product_ids: list[int] = Field(default_factory=list)
    products: list[RecommendationCandidate] = Field(default_factory=list)
    limit: int = Field(default=5, ge=1, le=20)


class RecommendationItem(ApiModel):
    rank: int
    product_id: int
    variant_id: int
    name: str
    brand: str | None = None
    category_name: str | None = None
    image_path: str | None = None
    sale_price: Decimal
    average_rating: Decimal
    review_count: int
    recommendation_score: float
    recommendation_reasons: list[str]


class MemberRecommendationResponse(ApiModel):
    member_id: int
    pet_id: int
    life_stage: LifeStage
    age_months: int
    items: list[RecommendationItem]

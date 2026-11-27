from datetime import date
from decimal import Decimal
from unittest import TestCase

from app.recommendation.rules import (
    determine_life_stage,
    matches_life_stage,
)
from app.recommendation.schemas import (
    LifeStage,
    PetProfile,
    PetType,
    RecommendationCandidate,
    RecommendationRequest,
)
from app.recommendation.service import recommend_products


REFERENCE_DATE = date(2026, 11, 17)


def pet(pet_type: PetType, birthday: date) -> PetProfile:
    return PetProfile(pet_id=1, pet_type=pet_type, birthday=birthday)


def product(
    product_id: int,
    life_stage: str,
    *,
    pet_type: str = "DOG",
    sold_out: bool = False,
    ingredient: str = "연어",
) -> RecommendationCandidate:
    return RecommendationCandidate(
        product_id=product_id,
        variant_id=product_id * 10,
        name=f"상품 {product_id}",
        category_id=1,
        sale_price=Decimal("15000"),
        average_rating=Decimal("4.7"),
        review_count=product_id * 3,
        sales_count=product_id * 10,
        sold_out=sold_out,
        pet_type=pet_type,
        main_ingredient=ingredient,
        life_stage=life_stage,
    )


class LifeStageRuleTest(TestCase):
    def test_dog_life_stage_boundaries(self) -> None:
        cases = [
            (date(2026, 6, 17), LifeStage.PUPPY),   # 5개월
            (date(2026, 5, 17), LifeStage.JUNIOR),  # 6개월
            (date(2025, 6, 17), LifeStage.JUNIOR),  # 17개월
            (date(2025, 5, 17), LifeStage.ADULT),   # 18개월
            (date(2019, 11, 17), LifeStage.SENIOR), # 84개월
        ]
        for birthday, expected in cases:
            with self.subTest(birthday=birthday):
                stage, _ = determine_life_stage(
                    pet(PetType.DOG, birthday),
                    REFERENCE_DATE,
                )
                self.assertEqual(stage, expected)

    def test_cat_life_stage_boundaries(self) -> None:
        cases = [
            (date(2025, 12, 17), LifeStage.JUNIOR), # 11개월
            (date(2025, 11, 17), LifeStage.ADULT),  # 12개월
            (date(2019, 11, 17), LifeStage.SENIOR), # 84개월
        ]
        for birthday, expected in cases:
            with self.subTest(birthday=birthday):
                stage, _ = determine_life_stage(
                    pet(PetType.CAT, birthday),
                    REFERENCE_DATE,
                )
                self.assertEqual(stage, expected)

    def test_all_ages_product_matches_every_stage(self) -> None:
        all_ages = product(1, "전연령")
        for stage in [
            LifeStage.PUPPY,
            LifeStage.JUNIOR,
            LifeStage.ADULT,
            LifeStage.SENIOR,
        ]:
            self.assertTrue(matches_life_stage(all_ages, stage))


class RecommendationPipelineTest(TestCase):
    def test_returns_life_stage_and_only_five_eligible_products(self) -> None:
        dog = PetProfile(
            pet_id=7,
            pet_type=PetType.DOG,
            birthday=date(date.today().year - 3, 1, 1),
            allergies=["닭"],
        )
        candidates = [product(index, "ADULT") for index in range(1, 7)]
        candidates.extend(
            [
                product(20, "PUPPY"),
                product(21, "ADULT", pet_type="CAT"),
                product(22, "ADULT", ingredient="닭고기"),
                product(23, "ADULT", sold_out=True),
            ]
        )
        request = RecommendationRequest(
            member_id=3,
            pet=dog,
            purchased_product_ids=[1],
            preferred_category_ids=[1],
            products=candidates,
        )

        response = recommend_products(request)

        self.assertEqual(response.life_stage, LifeStage.ADULT)
        self.assertEqual(len(response.items), 5)
        returned_ids = {item.product_id for item in response.items}
        self.assertNotIn(1, returned_ids)
        self.assertTrue(returned_ids.isdisjoint({20, 21, 22, 23}))

    def test_java_camel_case_contract(self) -> None:
        request = RecommendationRequest.model_validate(
            {
                "memberId": 3,
                "pet": {
                    "petId": 7,
                    "petType": "CAT",
                    "birthday": "2024-01-01",
                },
                "products": [],
            }
        )
        response = recommend_products(request)
        payload = response.model_dump(mode="json", by_alias=True)

        self.assertIn("lifeStage", payload)
        self.assertIn("ageMonths", payload)
        self.assertEqual(payload["petId"], 7)

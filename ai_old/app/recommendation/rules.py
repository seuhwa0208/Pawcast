"""생일 기반 생애주기 판정과 추천 제외 규칙입니다."""

from datetime import date
import re

from app.recommendation.schemas import LifeStage, PetProfile, PetType, RecommendationCandidate


def _age_in_months(birthday: date, reference_date: date) -> int:
    months = (reference_date.year - birthday.year) * 12 + reference_date.month - birthday.month
    if reference_date.day < birthday.day:
        months -= 1
    return max(months, 0)


def determine_life_stage(pet: PetProfile, reference_date: date | None = None) -> tuple[LifeStage, int]:
    age_months = _age_in_months(pet.birthday, reference_date or date.today())
    if pet.pet_type == PetType.DOG:
        if age_months < 6:
            return LifeStage.PUPPY, age_months
        if age_months < 18:
            return LifeStage.JUNIOR, age_months
    elif age_months < 12:
        return LifeStage.JUNIOR, age_months
    if age_months < 84:
        return LifeStage.ADULT, age_months
    return LifeStage.SENIOR, age_months


def parse_product_life_stages(value: str | None) -> set[LifeStage]:
    if not value:
        return {LifeStage.ALL_AGES}
    normalized = value.upper().replace("전연령", "ALL_AGES").replace("전 연령", "ALL_AGES")
    aliases = {stage.value: stage for stage in LifeStage}
    aliases["ALLAGES"] = LifeStage.ALL_AGES
    stages = {aliases[token] for token in re.split(r"[,/|+\s]+", normalized) if token in aliases}
    return stages or {LifeStage.ALL_AGES}


def matches_life_stage(product: RecommendationCandidate, life_stage: LifeStage) -> bool:
    stages = parse_product_life_stages(product.life_stage)
    return LifeStage.ALL_AGES in stages or life_stage in stages


def passes_recommendation_rules(product: RecommendationCandidate, pet: PetProfile, life_stage: LifeStage) -> bool:
    if product.pet_type.upper() not in {pet.pet_type.value, "ALL", "BOTH"}:
        return False
    if not matches_life_stage(product, life_stage):
        return False
    ingredient = (product.main_ingredient or "").casefold()
    return not any(allergy.casefold() in ingredient for allergy in pet.allergies if allergy.strip())

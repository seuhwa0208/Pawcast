# Spring Boot ↔ FastAPI 생애주기 추천 연동

## 처리 순서

1. 프론트에서 반려동물 생일을 등록한다.
2. Spring Boot가 `PETS.birthday`에 저장한다.
3. 추천 요청 시 Spring Boot가 로그인 회원의 반려동물과 후보 상품을 조회한다.
4. Spring Boot가 FastAPI `POST /api/recommendations`를 호출한다.
5. Python이 생일로 `lifeStage`를 계산한다.
6. 상품의 `FOOD_DETAILS.life_stage`와 비교하고 알레르기·대상 동물을 필터링한다.
7. 추천 점수를 계산하여 상위 5개와 `lifeStage`를 Spring Boot로 반환한다.
8. Spring Boot가 상품의 최신 가격·재고를 다시 확인한 후 프론트에 전달한다.

`PETS`에는 `life_stage` 컬럼을 추가하지 않는다. 생애주기는 날짜가 지나면 달라지는
파생값이므로 Python에서 요청 시점마다 계산한다.

## 생애주기 기준

| 동물 | 코드 | 월령 구간 |
| --- | --- | --- |
| 강아지 | `PUPPY` | 0~5개월 |
| 강아지 | `JUNIOR` | 6~17개월 |
| 강아지 | `ADULT` | 18~83개월 |
| 강아지 | `SENIOR` | 84개월 이상 |
| 고양이 | `JUNIOR` | 0~11개월 |
| 고양이 | `ADULT` | 12~83개월 |
| 고양이 | `SENIOR` | 84개월 이상 |
| 상품 전용 | `ALL_AGES` | 모든 생애주기 |

고양이는 `PUPPY`를 사용하지 않는다. `키튼` 상품 값은 Python에서 `JUNIOR`로
정규화한다. DB의 `FOOD_DETAILS.life_stage`는 가급적 위 영문 코드로 통일한다.

## Java → Python 요청 예시

```json
{
  "memberId": 21,
  "pet": {
    "petId": 15,
    "petType": "DOG",
    "birthday": "2024-05-12",
    "weightKg": 5.2,
    "allergies": ["닭고기"]
  },
  "purchasedProductIds": [3, 8],
  "wishProductIds": [12],
  "preferredCategoryIds": [1],
  "peerProductIds": [14, 18],
  "products": [
    {
      "productId": 101,
      "variantId": 1001,
      "name": "연어 어덜트 사료",
      "brand": "PAWCAST",
      "categoryId": 1,
      "categoryName": "사료",
      "imagePath": "/images/products/101.png",
      "salePrice": 24900,
      "averageRating": 4.8,
      "reviewCount": 120,
      "salesCount": 450,
      "soldOut": false,
      "petType": "DOG",
      "mainIngredient": "연어",
      "ingredientDescription": "연어, 감자, 오메가3",
      "lifeStage": "ADULT"
    }
  ],
  "limit": 5
}
```

## Python → Java 응답 예시

```json
{
  "memberId": 21,
  "petId": 15,
  "lifeStage": "ADULT",
  "ageMonths": 30,
  "items": [
    {
      "rank": 1,
      "productId": 101,
      "variantId": 1001,
      "name": "연어 어덜트 사료",
      "brand": "PAWCAST",
      "categoryName": "사료",
      "imagePath": "/images/products/101.png",
      "salePrice": 24900,
      "averageRating": 4.8,
      "reviewCount": 120,
      "recommendationScore": 89.4,
      "recommendationReasons": [
        "ADULT 생애주기 맞춤 상품",
        "선호 카테고리",
        "높은 구매자 평점"
      ]
    }
  ]
}
```

## Spring Boot DTO 핵심 필드

```java
public record AiRecommendationResponse(
    Long memberId,
    Long petId,
    LifeStage lifeStage,
    Integer ageMonths,
    List<RecommendedProduct> items
) {}

public enum LifeStage {
    PUPPY,
    JUNIOR,
    ADULT,
    SENIOR,
    ALL_AGES
}
```

Spring Boot는 Python이 반환한 상품 ID를 그대로 신뢰하지 말고, 응답 후 상품 상태와
옵션 재고를 다시 조회해야 한다. FastAPI 장애에는 타임아웃을 적용하고 인기 상품 같은
대체 응답을 사용할 것을 권장한다.

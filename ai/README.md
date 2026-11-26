# PAWCAST Sentiment AI

Vue가 직접 호출하는 PAWCAST 통합 FastAPI 서비스입니다.

- 리뷰 긍정·부정 분석 및 DB 저장
- 회원 반려동물 기반 상품 추천
- PostgreSQL 매출 집계·판매 예측·재학습
- 재고 부족 예측
- CSV·Excel 통계 다운로드

기존 리뷰·추천·매출 AI 프로젝트는 `D:\pawcast-fin\ai_old`에 보존되어 있습니다.

## 구조

```text
ai/
├── app/
│   ├── __init__.py
│   ├── main.py
│   ├── routers/
│   │   ├── __init__.py
│   │   └── predict.py
│   ├── services/
│   │   ├── __init__.py
│   │   └── predict_service.py
│   ├── schemas/
│   │   ├── __init__.py
│   │   ├── request.py
│   │   └── response.py
│   └── core/
│       ├── config.py
│       └── model_loader.py
├── models/
│   └── trained_model.pkl
├── tests/
├── .env
├── .env.example
├── .gitignore
├── requirements.txt
└── README.md
```

## 설치 및 실행

Windows CMD 또는 VS Code Command Prompt에서 실행합니다.

```cmd
cd /d D:\pawcast-fin\ai
py -3.12 -m venv .venv
call .venv\Scripts\activate.bat
python -m pip install -r requirements.txt
python -m pytest -q
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

Swagger 문서는 `http://127.0.0.1:8000/docs`에서 확인합니다.

Vue는 `.env`의 `VITE_AI_BASE_URL=http://127.0.0.1:8000`을 사용하며,
관리자 API 호출에는 Spring이 발급한 JWT를 `Authorization: Bearer ...` 형식으로 전달합니다.

주요 API:

```text
POST /predict
POST /api/reviews/{review_id}/analyze
GET  /api/reviews/sentiment-summary
POST /api/recommendations
GET  /api/sales/dashboard
POST /api/sales/retrain
GET  /api/sales/inventory-forecast
GET  /api/sales/export
```

## 예측 요청

```cmd
curl -X POST "http://127.0.0.1:8000/predict" -H "Content-Type: application/json" -d "{\"text\":\"배송이 빠르고 상품이 좋아요\"}"
```

응답 예시:

```json
{
  "label": "POSITIVE",
  "labelKo": "긍정",
  "confidence": 0.98,
  "probabilities": {
    "NEGATIVE": 0.02,
    "POSITIVE": 0.98
  },
  "modelName": "pawcast-review-sentiment-v1"
}
```

## 모델 정보

`trained_model.pkl`은 기존 `ai_old/data/review/ai_review_sentiment.csv`의 `train` 분할을 사용하여 학습합니다.

- 입력: 한국어 리뷰 본문
- 출력: `POSITIVE` 또는 `NEGATIVE`
- 특징 추출: 문자 단위 TF-IDF
- 분류기: Logistic Regression

## 리뷰 상품 5개 추리기

홈 화면의 **BEST 5 리뷰 상품**은 공개 리뷰의 평점과 AI 감성 분석 결과를
함께 사용해 상품과 대표 리뷰를 한 번에 조회합니다.

### 전체 흐름

1. `frontend/src/views/HomeView.vue`가 마운트될 때
   `productService.bestReviews({ ..., limit: 5 })`를 호출합니다.
2. `frontend/src/services/productService.js`가 Vue의 인증 헤더를 포함해
   `GET /api/reviews/best-products?limit=5`를 FastAPI에 직접 요청합니다.
3. `app/review/api.py`의 `best_review_products` 라우터가 요청을 받고,
   `app/review/service.py`의 `get_best_review_products`를 호출합니다.
4. 서비스는 `app/review/repository.py`의 `find_best_review_products` SQL 결과를
   `BestReviewProductResponse`, `BestReviewResponse` 응답 모델로 조립합니다.
5. HomeView는 받은 결과를 `bestReviewProducts` 상태에 저장하고 최대 5개를 렌더링합니다.

```text
HomeView.vue
  → productService.bestReviews(limit=5)
  → GET /api/reviews/best-products?limit=5
  → review/api.py
  → review/service.py
  → review/repository.py
  → PostgreSQL (products, product_reviews, review_sentiments)
  → BEST 5 리뷰 상품 렌더링
```

이 기능은 현재 Spring Boot를 경유하지 않습니다. Vue가 FastAPI에 직접 요청하고,
FastAPI가 PostgreSQL의 리뷰와 감성 분석 결과를 조회합니다.

### 베스트 상품 5개 선정 기준

`find_best_review_products`의 `top_products` CTE는 공개 리뷰
(`product_reviews.is_visible = TRUE`)가 하나 이상 있는 상품만 대상으로 합니다.

정렬 우선순위는 다음과 같습니다.

1. 평균 평점 `AVG(pr.rating)` 내림차순
2. 공개 리뷰 수 `COUNT(pr.id)` 내림차순
3. 상품 ID `p.id` 내림차순

즉, 평균 평점이 높은 상품을 먼저 고르고, 평점이 같으면 공개 리뷰가 많은 상품,
그마저 같으면 ID가 큰 상품을 우선합니다. 현재 ID는 생성 순서를 따르는 구조이므로
동률일 때 비교적 최근에 생성된 상품이 우선될 수 있습니다.

### 상품별 대표 리뷰 1건 선정 기준

`ranked_reviews` CTE는 선정된 각 상품 안에서
`ROW_NUMBER() OVER (PARTITION BY pr.product_id ...)`로 대표 리뷰 한 건을 고릅니다.

정렬 우선순위는 다음과 같습니다.

1. 별점 `pr.rating` 내림차순
2. AI 긍정도 `positive_probability` 내림차순
3. 작성일 `pr.created_at` 내림차순
4. 리뷰 ID `pr.id` 내림차순

AI 긍정도는 `review_sentiments`의 결과를 사용합니다.

- `POSITIVE`: `confidence`
- `NEGATIVE`: `1 - confidence`
- 분석 결과가 없거나 기존 레거시 값인 경우: `0`

별점이 높으면서 AI가 더 긍정적으로 판단한 리뷰를 먼저 보여 주고, 동률이면
최신 리뷰를 대표 리뷰로 선택하는 방식입니다.

### 설계 의도와 한계

- 리뷰 감성 분석 결과와 대표 리뷰 선정 규칙을 FastAPI 리뷰 도메인에 모아,
  홈 화면은 완성된 상품·리뷰 응답만 렌더링합니다.
- 상품과 대표 리뷰를 SQL의 CTE와 윈도우 함수로 함께 가져와 상품별 반복 조회를 줄입니다.
- 현재는 최소 리뷰 수 조건이 없습니다. 따라서 리뷰 1개인 5점 상품도 상위에 노출될 수 있습니다.
- 필요해지면 `HAVING COUNT(pr.id) >= :minimum_review_count` 또는 `inStockOnly` 같은
  요청 조건을 추가해 노출 기준을 강화할 수 있습니다.

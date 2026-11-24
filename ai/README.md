# PAWCAST Sentiment AI

리뷰 문장을 긍정 또는 부정으로 분류하는 독립 FastAPI 서비스입니다.

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

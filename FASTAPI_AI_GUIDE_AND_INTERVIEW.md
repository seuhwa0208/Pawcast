# PAWCAST FastAPI AI 전체 정리 및 실무 예상 질문

> 기준 경로: `D:\pawcast-fin\ai`  
> 작성 기준: 현재 저장된 코드 기준이며, 계획이 아니라 실제 구현 상태를 설명한다.

## 1. 서비스 역할

PAWCAST AI 서버는 FastAPI로 구현된 별도 애플리케이션이다. Vue가 FastAPI를 직접 호출하며, 필요한 경우 FastAPI가 PostgreSQL을 직접 조회·저장한다.

주요 기능은 다음과 같다.

1. 리뷰 감정 분석: 한국어 리뷰를 `POSITIVE` 또는 `NEGATIVE`로 분류한다.
2. 분석 결과 저장: `review_sentiments`에 분석 결과를 UPSERT한다.
3. 베스트 리뷰: 평균 평점 상위 상품을 뽑고, 상품별 대표 리뷰를 정렬 규칙으로 선정한다.
4. 맞춤 추천: 반려동물 종류·생애주기·알레르기 조건으로 후보를 필터링하고 점수화한다.
5. 매출 대시보드: 실제 주문·환불 데이터를 기간별로 집계한다.
6. 매출 예측 재계산: 최근 매출의 이동평균 기반 베이스라인을 계산한다.
7. 재고 예측: 최근 판매 속도와 현재 재고로 소진 예상일 및 권장 입고량을 계산한다.
8. 통계 내보내기: CSV, CSV ZIP, XLSX 파일을 생성한다.

## 2. 전체 요청 구조

```text
Vue 5174
  ├─ 일반 쇼핑몰 기능 ── JWT ──> Spring Boot 8080 ──> PostgreSQL
  └─ AI 기능 ───────── JWT ──> FastAPI 8000 ──────> PostgreSQL
                                      │
                                      └─> models/trained_model.pkl
```

- Vue는 `VITE_AI_BASE_URL`로 FastAPI 주소를 결정한다.
- 관리자 AI API에는 Spring이 발급한 JWT가 그대로 전달된다.
- FastAPI는 Spring과 동일한 `JWT_SECRET`, `JWT_ISSUER`로 토큰을 검증한다.
- DB 접근은 SQLAlchemy 비동기 세션과 `asyncpg`를 사용한다.
- 욕설 검증의 최종 책임은 Spring에 두는 것이 현재 프로젝트 방침이다. Python에 남은 욕설 모듈과 `/api/reviews/analyze`는 레거시 호환 성격이 있으므로 최종 정리가 필요하다.

## 3. 폴더별 책임

```text
ai/
├─ app/
│  ├─ main.py                 # FastAPI 생성, CORS, 라우터 조립, 종료 시 DB 풀 해제
│  ├─ api/router.py           # 리뷰·매출·추천 라우터 통합
│  ├─ common/
│  │  ├─ auth.py              # JWT 및 내부 API 키 검증
│  │  └─ database/            # async engine, session factory
│  ├─ core/
│  │  ├─ config.py            # .env 기반 설정
│  │  └─ model_loader.py      # pickle 모델 지연 로딩 및 캐시
│  ├─ routers/predict.py      # 단일 문장 감정 예측 API
│  ├─ services/predict_service.py
│  ├─ schemas/                # /predict 요청·응답 계약
│  ├─ review/                 # 리뷰 분석·저장·랭킹·통계
│  ├─ recommendation/         # 추천 후보·규칙·랭킹
│  └─ sales/                  # 매출·재고 예측·내보내기
├─ models/trained_model.pkl
├─ tests/
├─ .env
├─ .env.example
├─ requirements.txt
└─ README.md
```

## 4. 실행 방법

Windows CMD 또는 VS Code의 Command Prompt에서 실행한다.

```cmd
cd /d D:\pawcast-fin\ai
py -3.12 -m venv .venv
call .venv\Scripts\activate.bat
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
python -m pytest -q
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

확인 주소:

- Health: `http://127.0.0.1:8000/`
- Swagger: `http://127.0.0.1:8000/docs`
- OpenAPI JSON: `http://127.0.0.1:8000/openapi.json`

## 5. 사용 모델과 AI 라이브러리

### 감정 분석 모델

현재 실제 로딩되는 파일은 `models/trained_model.pkl`이며, 파일 크기는 약 150KB다. 모델 파일은 scikit-learn `Pipeline`으로 저장되어 있다.

#### 먼저 알아둘 용어

| 용어 | 쉬운 의미 | 이 프로젝트에서 하는 일 |
|---|---|---|
| 모델(Model) | 과거 데이터에서 규칙을 학습해 새 입력의 결과를 추정하는 프로그램 | 리뷰 문장을 받아 긍정/부정을 판단 |
| 학습(Training) | 정답이 붙은 예시를 보고 모델의 기준을 만드는 과정 | 긍정·부정 라벨이 달린 리뷰로 분류 기준 생성 |
| 추론(Inference) | 이미 학습된 모델에 새 데이터를 넣어 결과를 얻는 과정 | 사용자가 쓴 새 리뷰를 분석 |
| 특징(Feature) | 모델이 판단에 사용하는 숫자 정보 | 리뷰 문장에서 만든 문자 조각의 중요도 숫자 |
| 분류(Classification) | 정해진 범주 중 하나를 고르는 문제 | `POSITIVE` 또는 `NEGATIVE` 선택 |
| 확률/신뢰도(Confidence) | 모델이 선택한 결과를 얼마나 강하게 판단했는지 나타내는 값 | `0.91`이면 해당 예측 클래스에 91% 확률을 둔다는 뜻 |

여기서 모델은 ChatGPT처럼 문장을 새로 만들어내는 생성형 AI가 아니다. 정답이 있는 기존 리뷰를 보고 **긍정과 부정을 구분하도록 학습한 이진 분류기**다.

```text
Pipeline
├─ tfidf: TfidfVectorizer
│  ├─ analyzer = char_wb
│  ├─ ngram_range = (1, 3)
│  ├─ min_df = 2
│  └─ sublinear_tf = True
└─ classifier: LogisticRegression
   ├─ C = 3.0
   ├─ class_weight = balanced
   ├─ max_iter = 500
   └─ random_state = 42
```

모델이 분류하는 클래스는 `NEGATIVE`, `POSITIVE` 두 가지다.

#### 한 리뷰가 결과가 되기까지

```text
"배송이 빠르고 정말 좋아요"
          │
          ▼
TfidfVectorizer
문장을 "배", "배송", "배송이", "좋", "좋아", "좋아요" 같은 문자 패턴의 숫자 벡터로 변환
          │
          ▼
LogisticRegression
각 패턴이 긍정/부정일 가능성에 미치는 영향으로 두 확률 계산
          │
          ▼
POSITIVE 0.94 / NEGATIVE 0.06
          │
          ▼
최종 응답: POSITIVE, confidence=0.94
```

#### 구성 요소를 말로 풀면

| 구성 | 뜻 | 프로젝트에서의 해석 |
|---|---|---|
| `Pipeline` | 여러 처리 단계를 한 덩어리로 묶은 객체 | 리뷰 텍스트 변환과 분류를 항상 같은 순서로 실행 |
| `TfidfVectorizer` | 단어/문자 패턴을 모델이 읽을 수 있는 숫자로 바꾸는 도구 | 자주 나오지만 구별력 없는 패턴은 낮추고, 특정 감정에 도움이 되는 패턴은 높임 |
| TF | 한 리뷰 안에서 특정 패턴이 얼마나 자주 나오는지 | 예: “좋아”가 반복되면 그 리뷰에서는 더 중요한 힌트 |
| IDF | 모든 리뷰에 너무 흔한 패턴의 비중을 낮추는 값 | “상품”, “배송”처럼 거의 모든 리뷰에 있는 표현의 영향 완화 |
| n-gram | 연속된 n개 문자 또는 단어 묶음 | `좋`, `좋아`, `좋아요`처럼 길이가 다른 표현을 함께 사용 |
| `char_wb` | 단어 경계 안에서 문자 n-gram을 만드는 방식 | 한국어 띄어쓰기 실수와 짧은 표현 변화에 비교적 잘 견딤 |
| `LogisticRegression` | 입력 특징마다 가중치를 학습해 클래스별 확률을 계산하는 선형 분류기 | 어떤 문자 패턴이 긍정/부정에 가까운지 학습 후 확률 반환 |
| `class_weight=balanced` | 적은 클래스의 학습 손실에 더 큰 비중을 주는 설정 | 긍정 리뷰가 훨씬 많아도 부정을 무시하지 않게 보정 |
| `C=3.0` | 정규화 강도를 조절하는 값 | 너무 단순하거나 과하게 외우지 않도록 학습 복잡도를 조절 |
| `max_iter=500` | 최적화 반복의 최대 횟수 | 학습 계산이 충분히 수렴할 기회를 제공 |
| `random_state=42` | 난수 시드 | 같은 데이터와 코드에서 최대한 같은 결과를 재현 |

#### `.pkl` 파일은 무엇인가

`.pkl`은 Python의 `pickle` 형식이다. 학습을 마친 `Pipeline` 객체를 파일로 직렬화해 저장한 것이다. 서버가 시작된 뒤 처음 예측할 때 이 파일을 메모리에 불러오면, 다시 학습하지 않고 곧바로 추론할 수 있다.

다만 `pickle`은 신뢰할 수 없는 파일을 열면 보안 위험이 있다. 그래서 모델 파일은 팀이 직접 만든 artifact만 배포하고, 운영에서는 파일의 저장 경로·접근 권한·checksum을 관리해야 한다.

| 구성 | 실제 사용 방식 | 선택 이유 |
|---|---|---|
| `TfidfVectorizer` | 한국어 리뷰를 문자 단위 특징 벡터로 변환 | 띄어쓰기·조사·오타가 섞이는 짧은 한국어 리뷰에도 기본 토큰화보다 견고한 베이스라인 |
| `char_wb` | 단어 경계 내 문자 n-gram 사용 | 단어 내부 패턴을 유지하면서 다른 단어 경계를 넘는 불필요한 조합을 줄임 |
| 1~3-gram | 한 글자~세 글자 패턴 사용 | 감정 표현, 축약어, 부정 표현의 일부 패턴을 포착 |
| `LogisticRegression` | 이진 확률 분류 | `predict_proba`를 제공해 긍정/부정 confidence를 반환할 수 있고 CPU 추론이 빠름 |
| `class_weight=balanced` | 학습 시 클래스 가중치 자동 보정 | 긍정·부정 학습 데이터 수가 달라 한쪽으로 치우치는 문제 완화 |

면접에서는 “대형 언어 모델을 사용했다”고 말하면 안 된다. 현재 구현은 **scikit-learn 기반의 전통적 머신러닝 분류기**다. 장점은 적은 운영 자원, 빠른 CPU 추론, 쉬운 재현성과 설명 가능성이다. 한계는 문맥·풍자·긴 문장 이해가 Transformer보다 약하다는 점이다.

### 추천·매출·재고 기능의 모델 성격

| 기능 | 현재 방식 | ML 모델 여부 |
|---|---|---|
| 상품 추천 | 조건 필터 + 가중치 점수 + 정렬 | 규칙/랭킹 기반 |
| 매출 예측 | 최근 실적 기반 베이스라인 계산 | 단순 시계열 베이스라인 |
| 재고 예측 | 최근 판매량의 일평균과 재고 소진일 계산 | 수요 추정 규칙 |
| 리뷰 감정 | TF-IDF + Logistic Regression | 지도학습 이진 분류 |

따라서 `POST /api/sales/retrain`은 대규모 ML 재학습보다 최신 주문 데이터를 반영해 예측값을 다시 계산하는 작업에 가깝다.

## 6. 가상환경과 실제 패키지 버전

AI는 전역 Python과 분리된 `D:\pawcast-fin\ai\.venv` 가상환경에서 실행한다. 해당 가상환경은 시스템 site-packages를 사용하지 않으며, Python 3.12.0으로 생성되어 있다.

#### 가상환경이란 무엇인가

가상환경(virtual environment)은 프로젝트 전용 Python 실행 공간이다. Windows에 설치된 공용 Python은 그대로 두고, 이 프로젝트에 필요한 패키지와 버전만 `.venv` 폴더 안에 따로 설치한다.

```text
Windows 공용 Python
├─ 다른 프로젝트 A: fastapi 0.x, pandas 1.x
├─ 다른 프로젝트 B: django 등 다른 패키지
└─ PAWCAST AI .venv
   ├─ Python 3.12.0 기반 실행 파일
   ├─ FastAPI, scikit-learn, pandas 등 AI 전용 패키지
   └─ PAWCAST가 요구하는 버전 조합
```

가상환경을 쓰는 이유는 다음과 같다.

1. 다른 프로젝트의 패키지 버전 변경이 PAWCAST AI를 망가뜨리는 일을 막는다.
2. 팀원이 같은 `requirements.txt`로 비슷한 실행 환경을 만들 수 있다.
3. 삭제가 필요할 때 `.venv`만 지우고 다시 만들면 되므로 공용 Python이 오염되지 않는다.
4. VS Code 실행·테스트·서버 실행이 모두 같은 interpreter와 라이브러리를 사용한다.

`call .venv\Scripts\activate.bat`은 CMD에서 `python`, `pip` 명령이 공용 Python이 아니라 `.venv` 안의 Python을 가리키도록 PATH를 잠시 바꾸는 명령이다. 활성화하지 않아도 `.venv\Scripts\python.exe`를 직접 실행하면 같은 결과를 얻는다.

```cmd
cd /d D:\pawcast-fin\ai
py -3.12 -m venv .venv
call .venv\Scripts\activate.bat
python -m pip install -r requirements.txt
```

현재 설치를 확인한 주요 패키지 버전은 다음과 같다.

| 영역 | 패키지 | 설치 버전 | 역할 |
|---|---|---:|---|
| Runtime | Python | 3.12.0 | AI 서비스 실행 환경 |
| Web | FastAPI | 0.165.0 | API·Swagger·의존성 주입 |
| ASGI | Uvicorn | 0.54.0 | FastAPI 개발/실행 서버 |
| Validation | Pydantic | 2.14.4 | 요청·응답 DTO 검증 |
| Settings | pydantic-settings | 2.16.3 | `.env` 설정 로딩 |
| ML | scikit-learn | 1.9.1 | TF-IDF, Logistic Regression, pickle 모델 로딩 |
| Data | pandas | 2.3.3 | 매출 데이터 프레임·CSV/XLSX 생성 |
| DB ORM | SQLAlchemy | 2.1.9 | 비동기 SQL 실행·연결 관리 |
| DB Driver | asyncpg | 0.32.0 | PostgreSQL 비동기 드라이버 |
| Auth | PyJWT | 2.15.1 | Spring JWT 검증 |
| Excel | openpyxl | 3.1.5 | XLSX 파일 생성 |
| HTTP/Test | httpx | 0.28.1 | HTTP 테스트/연동 지원 |
| Test | pytest | 8.4.2 | 테스트 실행 |
| Env | python-dotenv | 1.2.4 | dotenv 호환 지원 |

`requirements.txt`는 호환 범위를 정의하고, 위 표는 현재 로컬 `.venv`의 실제 설치값이다. 배포 재현성을 높이려면 아래처럼 lock 파일도 생성해 함께 관리하는 편이 좋다.

```cmd
python -m pip freeze > requirements.lock.txt
```

## 7. 가상환경 운영 명령

```cmd
:: 가상환경 활성화
call .venv\Scripts\activate.bat

:: 현재 Python 확인
python --version

:: 현재 패키지 확인
python -m pip list

:: 테스트 실행
python -m pytest -q

:: 개발 서버 실행
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload

:: 가상환경 종료
deactivate
```

VS Code에서는 `D:\pawcast-fin\ai\.venv\Scripts\python.exe`를 Python Interpreter로 선택한다. 그래야 VS Code 실행, lint, pytest가 동일한 패키지 집합을 사용한다.

## 8. 환경변수

| 변수 | 역할 | 주의점 |
|---|---|---|
| `APP_NAME` | Swagger와 Health에 표시할 이름 | 기능 전체를 반영한 이름으로 변경 가능 |
| `MODEL_PATH` | 학습 모델 경로 | 실행 위치보다 절대 경로 해석 결과가 중요 |
| `MODEL_NAME` | DB와 응답에 남길 모델 버전명 | 재학습 시 버전 관리 권장 |
| `DATABASE_URL` | PostgreSQL async 연결 문자열 | `postgresql+asyncpg://` 사용 |
| `JWT_SECRET` | Spring JWT 서명 검증 키 | Spring과 반드시 동일해야 함 |
| `JWT_ISSUER` | JWT issuer | Spring 발급 값과 일치해야 함 |
| `INTERNAL_API_KEY` | 서버 간 내부 호출용 키 | 브라우저에 노출 금지 |
| `DB_POOL_SIZE` | 기본 연결 풀 크기 | DB max connection과 함께 조정 |
| `DB_MAX_OVERFLOW` | 순간 추가 연결 수 | 과도한 설정은 DB 고갈 가능 |
| `INVENTORY_FORECAST_*` | 재고 관측·리드·안전 기간 | 운영 정책에 맞춰 조절 |

실제 `.env`는 Git에 커밋하지 않고 `.env.example`에는 예시만 둔다.

## 9. API 목록

| Method | Path | 인증 | 역할 |
|---|---|---|---|
| GET | `/` | 없음 | Health check |
| POST | `/predict` | 없음 | 텍스트 감정 예측 |
| GET | `/api/reviews/best-products` | 없음 | 평점 상위 상품과 베스트 리뷰 |
| GET | `/api/reviews/products/{id}/ranked` | 없음 | 상품별 리뷰 AI 정렬 |
| GET | `/api/reviews/sentiment-summary` | ADMIN/MANAGER | 관리자 감정 통계 |
| POST | `/api/reviews/{id}/analyze` | 현재 없음 | 저장된 리뷰 분석 후 DB 저장 |
| POST | `/api/reviews/analyze` | 현재 없음 | 텍스트 분석 레거시 API |
| POST | `/api/recommendations` | 현재 없음 | 전달받은 후보 추천 |
| GET | `/api/sales/dashboard` | ADMIN/MANAGER | 매출 대시보드 |
| POST | `/api/sales/retrain` | ADMIN/MANAGER | 예측 베이스라인 재계산 |
| GET | `/api/sales/inventory-forecast` | ADMIN/MANAGER | 재고 소진 예측 |
| GET | `/api/sales/export` | ADMIN/MANAGER | CSV/XLSX 다운로드 |

### 보안상 중요한 현재 상태

리뷰 저장 분석과 추천 API에는 현재 `require_admin`이 없다. 리뷰 등록 직후 일반 회원이 분석을 호출해야 한다면, 단순 공개 대신 JWT에서 회원 ID를 확인하고 해당 리뷰 소유권 또는 호출 권한을 검증하는 방식이 적합하다. 추천 API도 브라우저가 보낸 `memberId`와 JWT subject가 일치하는지 검증해야 한다.

## 10. 리뷰 감정 분석 흐름

```text
Vue 리뷰 등록 → Spring이 product_reviews 저장
             → Vue가 reviewId로 FastAPI 호출
             → FastAPI가 공개 리뷰 조회
             → 모델 predict_proba 실행
             → POSITIVE/NEGATIVE와 confidence 산출
             → review_sentiments UPSERT
             → Vue가 분석 결과 표시
```

- 모델은 `pickle`로 로딩하며 `lru_cache(maxsize=1)`로 프로세스마다 한 번만 읽는다.
- CPU 추론은 `run_in_threadpool`로 실행하여 async 이벤트 루프 정지를 줄인다.
- `confidence`는 예측한 클래스 확률이다.
- 긍정 퍼센트는 긍정이면 confidence, 부정이면 `1-confidence` 개념으로 조회 쿼리에서 복원한다.
- DB 저장은 `ON CONFLICT(review_id) DO UPDATE`라 동일 리뷰 재분석이 중복 행을 만들지 않는다.

### 베스트 리뷰 선정 규칙

1. 상품 선정: 평균 별점 내림차순 → 리뷰 수 내림차순 → 상품 ID 내림차순
2. 상품별 리뷰 선정: 별점 내림차순 → 긍정 확률 내림차순 → 최신 작성일 → 리뷰 ID 내림차순
3. `ROW_NUMBER() OVER (PARTITION BY product_id ...)`로 상품별 1위를 선택한다.

## 11. 추천 시스템 흐름

```text
RecommendationRequest
  → 생일로 월령·생애주기 계산
  → 기본 후보 생성
  → 반려동물 종류·생애주기·알레르기 필터
  → 선호·찜·유사회원·평점·리뷰·판매량 점수
  → 정렬
  → 상위 limit개 반환
```

현재 후보 데이터는 FastAPI가 DB에서 조회하지 않고 요청의 `products` 배열로 받는다. 따라서 Vue 또는 별도 백엔드 계층이 신뢰 가능한 후보 데이터를 구성해야 한다. 장기적으로는 JWT의 회원 ID를 기준으로 FastAPI가 DB에서 반려동물과 후보를 직접 조회하거나, Spring 내부 API가 서명된 요청으로 후보를 전달하는 편이 무결성에 유리하다.

### 필터 규칙

- 이미 구매한 상품 제외
- 품절 제외
- 판매가 0 이하 제외
- 상품 ID 중복 제거
- DOG/CAT/ALL/BOTH 종류 일치
- 생애주기 일치 또는 `ALL_AGES`
- 주원료 문자열에 알레르기 원료가 포함되면 제외

### 점수 구성

- 정확한 생애주기: +40
- 전연령: +30
- 선호 카테고리: +15
- 찜 상품: +10
- 유사 회원 구매: +8
- 별점: `평점 × 4`
- 리뷰 수: 로그 보정, 최대 8
- 판매량: 로그 보정, 최대 7

로그 보정은 인기 상품의 큰 수치가 점수를 독점하는 현상을 완화한다.

## 12. 매출·재고 분석

매출 집계는 `PAID`, `PREPARING`, `SHIPPING`, `DELIVERED` 주문을 유효 매출로 본다. 완료 환불은 별도 환불 금액으로 집계한다.

- 대시보드: 총매출, 주문 수, 객단가, 환불액, 추이, 카테고리, 상위 상품
- 재학습: 이름은 retrain이지만 현재는 복잡한 ML 재학습보다 최근 실적 기반 예측 베이스라인 재계산에 가깝다.
- 재고 예측: `최근 판매량 / 관측일`로 일평균 판매량을 계산하고 `재고 / 일평균`으로 소진 예상일을 계산한다.
- 권장 입고량: `일평균 × (리드타임+안전기간) - 현재재고`, 최소 0
- 다운로드: 단일 CSV, 전체 CSV ZIP, XLSX 다중 시트 지원

## 13. 인증과 권한

`require_admin`은 두 방식을 지원한다.

1. Vue 직접 호출: `Authorization: Bearer <JWT>`
2. 서버 간 호출: `Internal-Api-Key`

JWT 검증 항목:

- HS256 서명
- issuer
- `type == access`
- `role ∈ {ADMIN, MANAGER}`

프론트 라우트 가드는 사용자 경험을 위한 1차 차단일 뿐이고, 실제 보안은 FastAPI/Spring 서버에서 다시 검사해야 한다.

## 14. 테스트 전략

현재 테스트는 Health, 예측 입력 검증, 감정 분석, 욕설 모듈, 추천 생애주기 경계, 추천 필터, camelCase 계약, CSV/XLSX 생성을 다룬다.

추가 권장 테스트:

1. JWT 없음·만료·변조·USER 역할의 401/403
2. 추천 memberId와 JWT subject 불일치
3. DB 트랜잭션 rollback
4. 분석 UPSERT 멱등성
5. 리뷰 정렬 동점 조건
6. 알레르기 동의어와 오탐
7. 미래 생일·윤년·월말 월령
8. 빈 매출 기간과 이전 기간 0원
9. 동시 재학습 요청
10. 대용량 XLSX 메모리 사용량

## 15. 운영·배포 체크리스트

- `DEBUG=False`
- 운영 Origin만 CORS 허용
- JWT secret과 DB 비밀번호는 Secret Manager 사용
- HTTPS 적용
- `/docs` 운영 공개 여부 결정
- 모델 파일 checksum 및 버전 기록
- DB migration으로 `review_sentiments` 보장
- 프로세스 수 × pool size가 DB 연결 한도를 넘지 않는지 계산
- 요청 ID, 처리시간, 모델 버전, 오류 코드를 구조화 로그로 기록
- Health와 readiness를 분리하고 DB/모델 준비 상태 확인
- 모델 교체 시 canary 또는 rollback 경로 확보

## 16. 현재 코드의 강점

- 도메인별 API·서비스·스키마·저장소 책임 분리
- 비동기 DB 접근과 연결 풀 사용
- CPU 추론을 thread pool로 분리
- 모델 지연 로딩
- Pydantic을 통한 입력 검증과 camelCase 계약
- SQL window function을 이용한 명확한 베스트 리뷰 선정
- UPSERT 기반 분석 멱등성
- 공유 export schema를 이용한 프론트/서버 다운로드 계약 통일

## 17. 우선 개선할 부분

1. 추천과 리뷰 저장 분석 API에 사용자 인증·소유권 검증 추가
2. 추천 후보를 브라우저 입력에 의존하지 않도록 서버 조회 구조로 변경
3. `retrain` 용어를 실제 ML 학습 여부에 맞게 `recalculate`로 바꾸거나 실제 학습 파이프라인 도입
4. pickle 역직렬화 파일의 출처·checksum 검증
5. 욕설 검증 책임을 Spring으로 단일화하고 Python 레거시 API 제거
6. 모델 성능 지표와 데이터 drift 모니터링
7. DB SQL을 repository 계층으로 더 일관되게 분리
8. OpenAPI 계약 테스트와 Vue 타입 생성 도입

## 18. 현직자 예상 질문과 답변

### 아키텍처

1. **왜 AI 기능을 Spring이 아니라 FastAPI로 분리했나요?**  
   Python의 scikit-learn·pandas 생태계를 직접 활용하고 모델 추론과 분석 배포 주기를 쇼핑몰 도메인과 분리하기 위해서다.

2. **왜 Vue가 FastAPI를 직접 호출하나요?**  
   AI 조회 요청에서 불필요한 Spring 프록시 홉을 줄이기 위해서다. 대신 JWT 검증, CORS, 오류 계약을 FastAPI가 직접 책임져야 한다.

3. **직접 호출의 단점은 무엇인가요?**  
   공개 엔드포인트가 늘고 브라우저가 두 백엔드 계약을 알아야 한다. 인증 정책과 오류 응답이 분산될 수 있다.

4. **Spring과 FastAPI의 경계를 어떻게 정했나요?**  
   주문·회원·결제 같은 트랜잭션 도메인은 Spring, 감정 분석·추천 점수·통계 계산은 FastAPI가 담당한다.

5. **DB를 두 서버가 같이 쓰면 위험하지 않나요?**  
   스키마 결합과 동시 수정 위험이 있다. AI는 가능한 읽기 중심으로 두고, 쓰기는 명확한 테이블과 멱등 키로 제한하며 migration 계약을 공유해야 한다.

### 비동기·성능

6. **async를 쓴 이유는 무엇인가요?**  
   DB I/O 대기 중 다른 요청을 처리하기 위해서다. CPU 추론까지 자동으로 빨라지는 것은 아니다.

7. **CPU 모델 추론은 왜 thread pool에 넣었나요?**  
   동기식 추론이 이벤트 루프를 막지 않게 하기 위해서다. 무거운 모델이라면 별도 worker나 task queue가 더 적합하다.

8. **모델은 요청마다 로딩하나요?**  
   아니다. `lru_cache`와 singleton analyzer로 프로세스당 한 번 지연 로딩한다.

9. **worker를 늘리면 모델 메모리는 어떻게 되나요?**  
   각 프로세스가 모델을 각각 보유하므로 worker 수만큼 메모리가 증가할 수 있다.

10. **DB pool 설정은 어떻게 정하나요?**  
    인스턴스 수 × worker 수 × pool size와 DB 최대 연결 수를 함께 계산하고 실제 동시 요청 지표로 조정한다.

### 모델

11. **현재 감정 모델은 무엇인가요?**  
    문자 단위 TF-IDF와 Logistic Regression 기반 이진 분류 모델이다.

12. **왜 Transformer를 쓰지 않았나요?**  
    데이터 규모와 운영 자원을 고려한 빠른 베이스라인이다. 지연시간과 설명 가능성이 좋고 CPU 배포가 쉽다.

13. **confidence는 정말 신뢰할 수 있나요?**  
    `predict_proba` 값이지만 완전히 보정된 확률이라고 단정할 수 없다. calibration curve, Brier score 등으로 검증해야 한다.

14. **중립이 없는 이유는 무엇인가요?**  
    현재 비즈니스 계약이 긍정·부정 이진 분류이기 때문이다. 애매한 결과는 낮은 confidence로 드러나며 필요하면 임계값 기반 보류 상태를 추가할 수 있다.

15. **모델 성능은 무엇으로 측정해야 하나요?**  
    Accuracy만 보지 않고 precision, recall, F1, confusion matrix와 클래스 불균형을 확인한다.

16. **pickle의 위험은 무엇인가요?**  
    신뢰할 수 없는 pickle은 로딩 중 임의 코드 실행 위험이 있다. 내부 artifact만 사용하고 checksum과 접근 권한을 관리해야 한다.

17. **모델 버전은 어떻게 추적하나요?**  
    응답과 `review_sentiments.model_name`에 버전을 저장한다. 데이터 버전·학습 코드 commit·metric도 함께 registry에 남기는 것이 좋다.

### 추천

18. **이 추천은 ML인가요?**  
    현재는 규칙 기반 필터와 가중 점수 기반 랭킹이다. 설명 가능하고 데이터가 적을 때 안정적이라는 장점이 있다.

19. **알레르기는 왜 hard filter인가요?**  
    선호 문제가 아니라 안전 조건이므로 점수 감점이 아니라 추천 후보에서 완전히 제외해야 한다.

20. **생애주기는 어떻게 계산하나요?**  
    생일과 기준일의 개월 차이를 계산하고 DOG/CAT별 경계값으로 PUPPY/JUNIOR/ADULT/SENIOR를 결정한다.

21. **품종을 직접 입력해도 괜찮나요?**  
    현재 추천 hard filter는 pet type, 생애주기, 알레르기이므로 품종 자유입력은 핵심 필터에 영향을 주지 않는다. 품종 기반 규칙을 추가할 때 표준화가 필요하다.

22. **왜 판매량과 리뷰 수에 log를 적용했나요?**  
    매우 인기 있는 상품의 큰 수치가 다른 적합성 신호를 압도하지 않도록 증가 폭을 완화하기 위해서다.

23. **콜드 스타트는 어떻게 처리하나요?**  
    반려동물 적합성과 전연령, 별점 등 콘텐츠·규칙 신호로 기본 추천을 만들고 행동 데이터가 쌓이면 선호 점수를 반영한다.

24. **추천 설명은 어떻게 만드나요?**  
    점수에 실제로 기여한 규칙을 reasons 배열에 누적해 응답한다.

25. **브라우저가 추천 후보를 조작하면요?**  
    현재 구조의 약점이다. 서버가 DB에서 후보를 조회하거나 내부 서명된 요청만 허용해야 한다.

### DB·정합성

26. **분석 결과가 중복 저장되지 않나요?**  
    `review_id` conflict를 기준으로 UPSERT하므로 한 리뷰당 한 최신 결과를 유지한다.

27. **분석 중 실패하면 어떻게 되나요?**  
    commit 전 예외는 세션 종료 시 반영되지 않는다. 명시적 rollback과 일관된 예외 로깅을 추가하면 더 안전하다.

28. **베스트 리뷰 동점은 어떻게 처리하나요?**  
    별점, 긍정도, 작성일, 리뷰 ID 순으로 완전한 결정 규칙을 둬 결과 재현성을 확보한다.

29. **환불 매출은 어떻게 처리하나요?**  
    유효 주문 매출과 `COMPLETED` 환불액을 별도로 집계한다. 순매출 정의가 필요하면 총매출에서 환불액을 명시적으로 차감해야 한다.

30. **N+1 문제는 없나요?**  
    주요 통계는 join과 CTE 한 번으로 조회한다. 쿼리 실행계획과 인덱스는 실제 데이터에서 확인해야 한다.

### 보안·운영

31. **CORS가 보안 기능인가요?**  
    브라우저 Origin 제약일 뿐 인증을 대신하지 않는다. 서버 권한 검증은 별도로 필요하다.

32. **Vue 라우트 가드만으로 관리자 API를 보호할 수 있나요?**  
    불가능하다. 사용자가 브라우저 요청을 직접 만들 수 있으므로 FastAPI가 JWT 역할을 검증해야 한다.

33. **401과 403 차이는 무엇인가요?**  
    토큰이 없거나 유효하지 않으면 401, 인증은 됐지만 역할이 부족하면 403이다.

34. **내부 API 키를 브라우저에서 사용하면 안 되는 이유는요?**  
    번들에 포함되어 누구나 확인할 수 있으므로 서버 간 secret으로만 사용해야 한다.

35. **재학습 API 동시 호출은 어떻게 막나요?**  
    현재 별도 lock이 없다. DB advisory lock, Redis distributed lock 또는 job queue로 단일 실행을 보장할 수 있다.

36. **대용량 Excel 다운로드의 문제는 무엇인가요?**  
    현재 메모리에 전체 파일을 만든다. 데이터가 커지면 streaming, 비동기 export job, object storage 링크 방식이 필요하다.

37. **모니터링할 지표는 무엇인가요?**  
    요청 수·오류율·p95 latency·DB pool 사용량·모델 추론시간·클래스 분포·낮은 confidence 비율·drift를 본다.

38. **무중단 모델 교체는 어떻게 하나요?**  
    버전별 artifact를 준비하고 새 인스턴스를 health 검증한 뒤 트래픽을 전환하며 이전 버전을 rollback 가능하게 유지한다.

39. **테스트 DB는 어떻게 구성해야 하나요?**  
    운영과 분리된 PostgreSQL에 migration과 fixture를 적용하고 테스트 종료 후 transaction rollback 또는 schema 재생성으로 격리한다.

40. **이 프로젝트에서 가장 먼저 개선할 한 가지는 무엇인가요?**  
    추천·리뷰 분석 API의 JWT subject 및 리소스 소유권 검증이다. 기능보다 데이터 조작 방지가 우선이다.


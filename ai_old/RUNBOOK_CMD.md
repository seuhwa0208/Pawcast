# PAWCAST AI 설치·검증·실행·배포 가이드

이 문서는 Windows CMD 또는 VS Code의 `Command Prompt` 터미널을 기준으로 작성되었습니다.

VS Code에서는 터미널 오른쪽의 `+` 옆 화살표를 누른 뒤 `Command Prompt`를 선택합니다.

## 1. AI 가상환경 생성

```cmd
cd /d D:\pawcast-fin\AI
py -3.11 -m venv .venv
```

가상환경을 활성화합니다.

```cmd
.venv\Scripts\activate.bat
```

정상적으로 활성화되면 명령줄 앞에 `(.venv)`가 표시됩니다.

```text
(.venv) D:\pawcast-fin\AI>
```

가상환경을 종료할 때는 다음 명령을 사용합니다.

```cmd
deactivate
```

## 2. Python 패키지 설치

가상환경을 활성화한 상태에서 실행합니다.

```cmd
python -m pip install --upgrade pip
pip install -r requirements.txt
pip install pytest httpx
```

설치 상태를 확인합니다.

```cmd
python --version
pip check
```

정상이면 다음 메시지가 출력됩니다.

```text
No broken requirements found.
```

## 3. AI 환경변수 설정

`.env`가 없다면 예제 파일을 복사합니다.

```cmd
cd /d D:\pawcast-fin\AI
copy .env.example .env
code .env
```

`AI\.env`의 주요 설정 예시는 다음과 같습니다.

```dotenv
APP_NAME=PAWCAST AI Server
DEBUG=True

MODEL_PATH=./models/review/sentiment_model
MODEL_NAME=WhitePeak/bert-base-cased-Korean-sentiment
MAX_LENGTH=256

DATABASE_URL=postgresql+asyncpg://postgres:실제비밀번호@127.0.0.1:5432/pawcast
DB_POOL_SIZE=5
DB_MAX_OVERFLOW=10
DB_POOL_TIMEOUT=30
DB_ECHO=False

JWT_SECRET=백엔드와_동일한_32바이트_이상_비밀키
JWT_ISSUER=pawcast
```

주의 사항:

- `DATABASE_URL`은 Python용 `postgresql+asyncpg://` 형식으로 작성합니다.
- DB 비밀번호에 `@`, `#`, `/` 등의 특수문자가 있으면 URL 인코딩해야 합니다.
- `JWT_SECRET`과 `JWT_ISSUER`는 Spring Boot 설정과 반드시 같아야 합니다.
- 실제 `.env` 파일은 Git에 커밋하지 않습니다.

## 4. PostgreSQL 준비

PostgreSQL과 `pawcast` 데이터베이스가 먼저 실행되어 있어야 합니다.

`psql`이 설치되어 있다면 다음 명령으로 접속을 확인합니다.

```cmd
psql -h 127.0.0.1 -U postgres -d pawcast
```

`psql`이 없다면 pgAdmin에서 다음 항목을 확인합니다.

- 서버: `127.0.0.1`
- 포트: `5432`
- 데이터베이스: `pawcast`
- 사용자 및 비밀번호가 `.env` 설정과 일치하는지 확인
- 회원, 상품, 리뷰, 주문 관련 테이블이 존재하는지 확인

AI 서버는 백엔드 테이블을 생성하지 않습니다. Spring Boot를 먼저 한 번 실행하거나 프로젝트 SQL을 적용하여 테이블을 준비해야 합니다.

## 5. AI 데이터 검증

```cmd
cd /d D:\pawcast-fin\AI
.venv\Scripts\activate.bat
python scripts\validate_ai_datasets.py
```

검증 결과는 다음 위치에 저장됩니다.

```text
D:\pawcast-fin\AI\evaluation_results\dataset_validation.json
```

현재 데이터 구성은 다음과 같습니다.

- 리뷰 감정 데이터: 9,454건
- 추천 상호작용: 49,272건
- 상품: 2,730건
- 회원: 3,997건
- 재구매: 11,735건
- 일별 매출: 1,095건
- 상품군별 매출: 12,869건

추천 데이터에는 회원 마스터에 없는 `member_id=2`의 상호작용 172건이 있습니다. 학습 시 해당 행을 제외하거나 콜드스타트 회원으로 처리해야 합니다.

다음 파일은 사후 검증용이므로 학습에 사용하면 안 됩니다.

```text
AI\data\reference\DESIGN_TRUTH_do_not_train.csv
```

## 6. 감정 분석 모델 준비

모델 폴더를 확인합니다.

```cmd
dir models\review\sentiment_model
```

모델이 없거나 폴더가 비어 있다면 다운로드합니다.

```cmd
python scripts\review\download_model.py
```

다운로드한 모델을 로딩할 수 있는지 검사합니다.

```cmd
python scripts\review\check_model.py
```

정상적인 경우 다음과 같은 내용이 출력됩니다.

```text
모델 로딩 성공
분류 클래스 수: 2
```

현재 리뷰 모델은 `긍정/부정` 2분류 모델입니다.

## 7. 감정 분석 모델 평가

기본 `test` 데이터로 평가합니다.

```cmd
python scripts\review\evaluate_model.py
```

분할별 평가 명령은 다음과 같습니다.

```cmd
python scripts\review\evaluate_model.py --split valid
python scripts\review\evaluate_model.py --split test
python scripts\review\evaluate_model.py --split ood_test
python scripts\review\evaluate_model.py --split all
```

`all`은 학습 데이터를 제외하고 `valid`, `test`, `ood_test`를 합쳐 평가합니다.

평가 결과는 다음 위치에 생성됩니다.

```text
AI\evaluation_results\review\metrics.json
AI\evaluation_results\review\all_predictions.csv
AI\evaluation_results\review\misclassified_reviews.csv
```

## 8. Python 테스트 실행

전체 테스트를 실행합니다.

```cmd
cd /d D:\pawcast-fin\AI
pytest -v
```

기능별 테스트 명령은 다음과 같습니다.

```cmd
pytest tests\review -v
pytest tests\recommendation -v
pytest tests\sales -v
```

## 9. FastAPI 개발 서버 실행

VS Code의 첫 번째 CMD 터미널에서 실행합니다.

```cmd
cd /d D:\pawcast-fin\AI
.venv\Scripts\activate.bat
python -m uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```

확인 주소:

- 서버 상태: `http://127.0.0.1:8000`
- Swagger: `http://127.0.0.1:8000/docs`
- 리뷰 테스트 화면: `http://127.0.0.1:8000/test`

CMD에서 서버 응답을 확인할 수 있습니다.

```cmd
curl http://127.0.0.1:8000/
```

리뷰 분석 API를 직접 검사하는 명령은 다음과 같습니다.

```cmd
curl -X POST "http://127.0.0.1:8000/api/reviews/analyze" -H "Content-Type: application/json" -d "{\"content\":\"배송도 빠르고 상품도 마음에 들어요\"}"
```

## 10. 기존 DB 리뷰 일괄 분석

이 작업은 DB의 리뷰 분석 결과를 변경하므로 먼저 데이터베이스를 백업합니다.

지원 옵션을 확인합니다.

```cmd
python scripts\review\backfill_sentiments.py --help
```

처음에는 적은 수의 리뷰만 처리하여 결과를 확인한 후 전체 리뷰를 처리합니다.

처리 구조는 다음과 같습니다.

```text
Python이 미분석 리뷰를 DB에서 조회
→ 감정 모델로 긍정/부정 분석
→ 긍정 확률과 분석 결과를 DB에 저장
→ Vue가 FastAPI 조회 API로 결과 요청
```

욕설 검증은 Spring에서 리뷰를 등록하거나 수정하기 전에 수행합니다. Python 감정 분석 과정에서는 욕설 검증을 담당하지 않습니다.

## 11. Spring Boot 설정 및 실행

VS Code의 두 번째 CMD 터미널을 엽니다.

```cmd
cd /d D:\pawcast-fin\backend
```

`.env`가 없다면 생성합니다.

```cmd
copy .env.example .env
code .env
```

주요 설정 예시는 다음과 같습니다.

```dotenv
DB_URL=jdbc:postgresql://127.0.0.1:5432/pawcast
DB_USERNAME=postgres
DB_PASSWORD=실제비밀번호

JWT_SECRET=AI와_동일한_32바이트_이상_비밀키
JWT_ISSUER=pawcast

AI_BASE_URL=http://127.0.0.1:8000
```

백엔드 테스트를 실행합니다.

```cmd
gradlew.bat clean test
```

백엔드를 실행합니다.

```cmd
gradlew.bat bootRun
```

백엔드 기본 주소는 `http://127.0.0.1:8080`입니다.

## 12. Vue 프론트엔드 설정 및 실행

VS Code의 세 번째 CMD 터미널을 엽니다.

```cmd
cd /d D:\pawcast-fin\frontend
```

`.env`가 없다면 생성합니다.

```cmd
copy .env.example .env
code .env
```

설정값은 다음과 같습니다.

```dotenv
VITE_SPRING_BASE_URL=http://127.0.0.1:8080
VITE_AI_BASE_URL=http://127.0.0.1:8000
```

패키지를 설치하고 개발 서버를 실행합니다.

```cmd
npm install
npm run dev
```

프론트엔드 주소는 `http://127.0.0.1:5174`입니다.

## 13. VS Code 터미널별 실행 명령

### 터미널 1: FastAPI

```cmd
cd /d D:\pawcast-fin\AI
.venv\Scripts\activate.bat
python -m uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```

### 터미널 2: Spring Boot

```cmd
cd /d D:\pawcast-fin\backend
gradlew.bat bootRun
```

### 터미널 3: Vue

```cmd
cd /d D:\pawcast-fin\frontend
npm run dev
```

전체 실행 순서는 다음과 같습니다.

```text
PostgreSQL
→ FastAPI
→ Spring Boot
→ Vue
```

## 14. 전체 연동 검증

다음 순서로 확인합니다.

1. PostgreSQL이 실행 중인지 확인합니다.
2. FastAPI의 `/`와 `/docs`가 열리는지 확인합니다.
3. Spring Boot가 8080 포트에서 실행되는지 확인합니다.
4. Vue가 5174 포트에서 실행되는지 확인합니다.
5. 회원 로그인 후 JWT가 발급되는지 확인합니다.
6. 욕설이 들어간 리뷰가 Spring에서 차단되는지 확인합니다.
7. 정상 리뷰가 DB에 저장되는지 확인합니다.
8. Python 분석 후 감정 결과가 DB에 저장되는지 확인합니다.
9. 베스트 리뷰 상품 5개가 화면에 표시되는지 확인합니다.
10. 회원 추천 상품이 화면에 표시되는지 확인합니다.
11. 관리자 매출 대시보드와 CSV 다운로드를 확인합니다.

대표 AI API는 다음과 같습니다.

```text
GET  /api/reviews/best-products
POST /api/reviews/{review_id}/analyze
POST /api/reviews/analyze

POST /api/recommendations

GET  /api/sales/dashboard
POST /api/sales/retrain
GET  /api/sales/export
```

## 15. 배포 전 최종 검증

### AI

```cmd
cd /d D:\pawcast-fin\AI
.venv\Scripts\activate.bat
pip check
python scripts\validate_ai_datasets.py
python scripts\review\check_model.py
python scripts\review\evaluate_model.py --split test
pytest -v
```

### Spring Boot

```cmd
cd /d D:\pawcast-fin\backend
gradlew.bat clean test
gradlew.bat build
```

생성된 JAR 파일은 `D:\pawcast-fin\backend\build\libs`에 저장됩니다.

### Vue

```cmd
cd /d D:\pawcast-fin\frontend
npm ci
npm run build
```

프론트엔드 빌드 결과는 `D:\pawcast-fin\frontend\dist`에 저장됩니다.

## 16. 운영용 실행

운영 환경에서는 FastAPI의 `--reload` 옵션을 제거합니다.

```cmd
cd /d D:\pawcast-fin\AI
.venv\Scripts\activate.bat
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --workers 1
```

감정 모델은 프로세스마다 메모리에 적재되므로 처음에는 `--workers 1`로 실행하는 것이 안전합니다.

Spring Boot JAR를 실행합니다.

```cmd
cd /d D:\pawcast-fin\backend
java -jar build\libs\생성된파일명.jar
```

운영용 `AI\.env`에서는 디버그 모드를 끕니다.

```dotenv
DEBUG=False
```

외부 배포 시 8000번과 8080번 포트를 직접 공개하지 말고, Nginx 등의 리버스 프록시와 HTTPS를 적용하는 것이 안전합니다.

```text
사용자
→ HTTPS / Reverse Proxy
  → Vue 정적 파일
  → Spring Boot:8080
  → FastAPI:8000
```

## 17. 자주 발생하는 문제

### `python` 또는 `py`를 찾을 수 없는 경우

Python 3.11을 설치하고 설치 과정에서 `Add Python to PATH`를 선택합니다.

### `ModuleNotFoundError`가 발생하는 경우

현재 CMD에 `(.venv)`가 표시되는지 확인한 뒤 다시 설치합니다.

```cmd
cd /d D:\pawcast-fin\AI
.venv\Scripts\activate.bat
pip install -r requirements.txt
```

### DB 연결 오류가 발생하는 경우

- PostgreSQL 실행 여부
- DB 이름, 사용자, 비밀번호
- `DATABASE_URL` 형식
- 비밀번호 특수문자 URL 인코딩 여부
- 5432 포트 사용 여부

를 확인합니다.

### 모델 파일 오류가 발생하는 경우

```cmd
python scripts\review\download_model.py
python scripts\review\check_model.py
```

### 8000 포트가 이미 사용 중인 경우

사용 중인 프로세스를 확인합니다.

```cmd
netstat -ano | findstr :8000
```

표시된 PID를 확인한 후, 본인이 실행한 불필요한 프로세스인 경우에만 종료합니다.

```cmd
taskkill /PID 실제PID /F
```

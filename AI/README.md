# PAWCAST AI

리뷰 분석, 매출 분석, 상품 추천을 독립된 도메인으로 관리하는 FastAPI 프로젝트입니다.

## 디렉터리 구조

```text
reviewAI/
├─ app/
│  ├─ api/                 # 도메인 라우터 조합
│  ├─ common/              # 공통 설정과 유틸리티
│  ├─ review/              # 욕설 필터·리뷰 감정 분석
│  ├─ sales/               # 매출 집계·판매 예측
│  └─ recommendation/      # 상품 후보 생성·필터·랭킹
├─ data/
│  └─ review/              # 리뷰 평가 데이터
├─ evaluation_results/
│  └─ review/              # 리뷰 모델 평가 결과
├─ models/
│  └─ review/              # 리뷰 감정 분석 모델
├─ jobs/                   # 집계·예측·추천·재학습 배치 작업
├─ scripts/
│  └─ review/              # 모델 다운로드·검증·평가 스크립트
├─ static/
│  └─ review/              # 리뷰 API 시험 화면
├─ tests/
│  └─ review/              # 리뷰 도메인 테스트
└─ main.py
```

## 실행

프로젝트 루트에서 실행합니다.

```bash
uvicorn main:app --reload
```

## 리뷰 테스트

```bash
python -m unittest discover -s tests -p "test_*.py"
python scripts/review/evaluate_model.py
python scripts/review/check_model.py
```

매출과 추천 도메인은 리뷰 도메인의 내부 구현에 직접 의존하지 않습니다. 필요한 분석 결과는 DTO 또는 공통 feature 계층을 통해 전달하도록 구현합니다.

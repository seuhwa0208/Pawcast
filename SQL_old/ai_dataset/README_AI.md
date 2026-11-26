# Pawcast AI 학습/검증용 더미 데이터 (CSV)

전부 **합성 데이터**입니다. 실제 고객 데이터가 아니므로 여기서 나온 정확도는 "파이프라인이 돌아가는지, 모델끼리 비교가 되는지" 확인하는 용도입니다. 실서비스 성능으로 보고하면 안 됩니다.
기간: 2023-11-21 ~ 2026-11-20 (마지막 날은 부분 집계라 매출 CSV에서 제외). 인코딩: UTF-8 BOM(utf-8-sig) → 엑셀에서 바로 열림.

## 파일 (모델 학습 총 소요 약 수 분~수십 분 규모)
| 파일 | 행 | 용도 | 분할 기준 |
|---|---|---|---|
| ai_sales_daily.csv | 1,095 | 일별 매출 분석/예측 (orders, net_sales, 달력·이벤트 특성) | train ≤2026-03-31, valid 04~07월, test 08월~ |
| ai_sales_daily_by_group.csv | 12,869 | 상품군(11)·타입(DOG/CAT/DEVICE)별 일별 판매 | 동일 |
| ai_calendar_future.csv | 100 | 미래 100일 달력/계획 이벤트 (정답 없음, 예측 입력용) | - |
| ai_review_sentiment.csv | 11,468 | 리뷰 감성 3분류 (label=본문 감성) | train/valid/test(시간) + **ood_test**(학습에 없는 문구 포함 리뷰) |
| ai_review_gold_candidates_blank.csv | 402 | 사람이 직접 라벨링할 검수용 샘플 (human_label 비어 있음) | - |
| ai_member_repurchase.csv | 11,735 | 회원 90일 내 재구매 예측 (5개 시점 스냅샷) | 스냅샷 날짜로 train/valid/test |
| ai_interactions.csv | 49,272 | 추천용 구매·찜 로그 | 시간 기준 동일 |
| ai_products.csv / ai_members.csv | 2,730 / 3,997 | 추천 입력 특성 | - |
| BASELINE_RESULTS.json | - | 기준선 모델 결과 (아래) | - |
| DESIGN_TRUTH_do_not_train.csv | 1,095 | 생성기가 의도한 기대 주문수. **학습 금지**, 예측이 구조를 맞췄는지 사후 확인용 | - |

### 주의할 점
- 리뷰 `label`은 **본문 기준**입니다. 약 4%(471건)는 평점과 본문 감성이 일부러 어긋나게 만들었습니다(평점만 믿는 규칙은 이 건들에서 0% 정답).
- `ood_test`는 문구 일부가 학습 분포에서 빠진 평가용입니다. 학습에 쓰지 마세요.
- 매출 예측 시 `event_groups`는 **미리 알 수 있는 이벤트(달력)**만 입력하세요. 서버 장애·SNS 화제 같은 돌발 이벤트는 train 구간에만 있고, 예측 구간 입력으로는 알 수 없는 정보입니다.
- 재구매 특성은 모두 스냅샷 날짜 이전 정보만 사용했습니다(미래 정보 누수 없음).
- 추천 입력의 상품군/브랜드 등은 카탈로그에서 파생한 값이며 DB 컬럼이 아닙니다.

## 기준선 결과 (이 데이터에서 나온 값, 합성이라 낙관적)
- 매출 예측(WAPE, 낮을수록 좋음): test 0.25 (Ridge+달력/이벤트) vs 0.31 (최근 4주 요일평균). 이벤트일만: 0.245 vs 0.353 → 이벤트 정보를 쓰면 개선되는 구조가 반영됨.
- 감성 분류(macro-F1): valid 0.98 / test 0.96 / ood 0.95. 합성 문장이라 쉬운 편 → 실제 리뷰로는 훨씬 낮을 수 있음.
- 재구매 AUC: valid 0.95 / test 0.94 (recency만으로도 0.93 → 합성 패턴이 단순함).
- 추천 Hit@10: 인기순 0.20 vs 공출현 0.24.
→ 직접 만든 모델은 위 기준선보다 나은지 비교하는 용도로 쓰시면 됩니다.

## 실행 방법
### CSV 열기
- 엑셀: 그냥 더블클릭 (BOM 포함이라 한글 깨지지 않음). 깨지면 데이터 → 텍스트/CSV 가져오기 → UTF-8.
- 파이썬: `import pandas as pd; df = pd.read_csv("ai_review_sentiment.csv", encoding="utf-8-sig")`
- DB에 넣기(선택): psql에서 `\copy` 사용. 단, 이 CSV들은 분석용이라 DB 테이블과 1:1이 아닙니다.

### 학습 예시 (감성)
```python
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
d = pd.read_csv("ai_review_sentiment.csv", encoding="utf-8-sig")
tr = d[d.split=="train"]; v = TfidfVectorizer(analyzer="char_wb", ngram_range=(1,3))
m = LogisticRegression(max_iter=300).fit(v.fit_transform(tr.content), tr.label)
```

### 기준선 다시 돌리기
```
python validate_baselines.py ai_dataset      # pandas, scikit-learn 필요
```

### 데이터 다시 생성하기 (SQL + CSV 한 번에)
```
python gen_pawcast_seed.py --seed-file ef797a95-pawcast_final_seed.sql --out pawcast_seed_3y.sql --ai-dir ai_dataset
```
(`pawcast_review_text.py`, `ai_export.py`가 같은 폴더에 있어야 합니다. 같은 `--rng` 값이면 같은 결과가 나옵니다.)

### SQL 파일
psql 또는 SQL 에디터에서 `pawcast_init.sql` → `pawcast_seed_3y.sql` 순서로 실행. 리뷰 라벨을 review_sentiments에 넣고 싶을 때만 `pawcast_review_sentiments_optional.sql` 실행.

### JSON / py 확장자 설명
- `.json`: 실행하는 파일이 아니라 요약 정보입니다. 메모장이나 VS Code로 열어 보면 됩니다.
- `.py`: `python 파일명.py` 로 실행합니다 (cmd에서 `python --version`이 나오면 준비 완료).

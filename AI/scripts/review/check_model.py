from pathlib import Path

from transformers import (
    # 한국어 문장을 AI가 읽을 수 있게 토큰으로 나눈 후 숫자로 변환
    AutoTokenizer,
    # 문장 맥락을 파악해 특정 카테고리로 분류하는 작업 -> 긍정과 부정을 판단함
    AutoModelForSequenceClassification
)

PROJECT_ROOT = Path(__file__).resolve().parents[2]
MODEL_DIR = PROJECT_ROOT / "models" / "review" / "sentiment_model"

print("토크나이저 로딩")
# from_pertrained: 토크나이저와 모델의 설정값, 가중치를 불러오는 메서드
tokenizer = AutoTokenizer.from_pretrained(
    str(MODEL_DIR),
    # 내 컴퓨터 로컬 폴더에 있는 파일만 사용
    local_files_only = True
)

print("모델 로딩")
model = AutoModelForSequenceClassification.from_pretrained(
    str(MODEL_DIR),
    local_files_only = True
)

print("모델 로딩 성공")

print("모델 클래스:", model.__class__.__name__)
print("분류 클래스 수:", model.config.num_labels)
print("레이블 정보:", model.config.id2label)

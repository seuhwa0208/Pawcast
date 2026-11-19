from pathlib import Path
from huggingface_hub import snapshot_download

# 모델 ID
MODEL_ID = "WhitePeak/bert-base-cased-Korean-sentiment"

# 다운로드할 디렉터리
PROJECT_ROOT = Path(__file__).resolve().parents[2]
MODEL_DIR = PROJECT_ROOT / "models" / "review" / "sentiment_model"

# 상위 폴더 및 기 생성 유무와 상관없이 진행
MODEL_DIR.mkdir(parents=True, exist_ok=True)

print("한국어 감정 분석 모델 다운로드 시작")


download_path = snapshot_download(
    repo_id = MODEL_ID,
    # 문자열 경로로 변환해서 넘기기
    local_dir = str(MODEL_DIR)   
)

print("모델 다운로드 완료")
print(f"저장 위치: {download_path}")

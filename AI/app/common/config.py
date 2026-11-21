from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


PROJECT_ROOT = Path(__file__).resolve().parents[2]

class Settings(BaseSettings):
    APP_NAME: str = "PAWCAST AI Server"
    
    # 디버그 모드 활성화
    DEBUG: bool = True

    # PostgreSQL read access for analytics and recommendation features.
    DATABASE_URL: str = (
        "postgresql+asyncpg://postgres:postgres@127.0.0.1:5432/pawcast"
    )
    DB_POOL_SIZE: int = 5
    DB_MAX_OVERFLOW: int = 10
    DB_POOL_TIMEOUT: int = 30
    DB_ECHO: bool = False
    INTERNAL_API_KEY: str = ""
    JWT_SECRET: str = ""
    JWT_ISSUER: str = "pawcast"

    # 로컬 데모 데이터와 재학습 조건 설정입니다.
    USE_MOCK_DATA: bool = False
    MOCK_RANDOM_SEED: int = 20261119
    MOCK_SALES_MONTHS: int = 18
    RETRAINING_ORDER_THRESHOLD: int = 100
    RETRAINING_MINIMUM_DAYS: int = 7
    
    # 모델은 미리 내려받은 로컬 디렉터리에서만 불러옵니다.
    MODEL_PATH: Path = PROJECT_ROOT / "models" / "review" / "sentiment_model"
    MODEL_NAME: str = "WhitePeak/bert-base-cased-Korean-sentiment"
    
    # 텍스트 토큰화 시 처리할 최대 길이 지정
    MAX_LENGTH: int = 256
    
    model_config = SettingsConfigDict(
        # Spring JWT 검증값은 backend/.env에서 공유하고 AI/.env가 있으면 우선 적용합니다.
        env_file=(PROJECT_ROOT.parent / "backend" / ".env", PROJECT_ROOT / ".env"),
        env_file_encoding="utf-8",
        # .env 파일에 정의되지 않은 여분의 값이 있어도 에러 없이 무시하도록 설정합니다.
        extra="ignore",
        case_sensitive=False,
    )
    
settings = Settings()

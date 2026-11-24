from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


PROJECT_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    APP_NAME: str = "PAWCAST Sentiment AI"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False
    MODEL_NAME: str = "pawcast-review-sentiment-v1"
    MODEL_PATH: Path = PROJECT_ROOT / "models" / "trained_model.pkl"

    model_config = SettingsConfigDict(
        env_file=PROJECT_ROOT / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )


settings = Settings()

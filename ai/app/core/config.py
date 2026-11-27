from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


PROJECT_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    APP_NAME: str = "PAWCAST Sentiment AI"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False
    MODEL_NAME: str = "pawcast-review-sentiment-v1"
    MODEL_PATH: Path = PROJECT_ROOT / "models" / "trained_model.pkl"
    MAX_LENGTH: int = 1000

    DATABASE_URL: str = "postgresql+asyncpg://postgres:postgres@127.0.0.1:5432/pawcast"
    DB_POOL_SIZE: int = 5
    DB_MAX_OVERFLOW: int = 10
    DB_POOL_TIMEOUT: int = 30
    DB_ECHO: bool = False

    JWT_SECRET: str = ""
    JWT_ISSUER: str = "pawcast"
    INTERNAL_API_KEY: str = ""

    INVENTORY_FORECAST_LOOKBACK_DAYS: int = 28
    INVENTORY_FORECAST_LEAD_DAYS: int = 7
    INVENTORY_FORECAST_SAFETY_DAYS: int = 14

    model_config = SettingsConfigDict(
        env_file=PROJECT_ROOT / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )


settings = Settings()

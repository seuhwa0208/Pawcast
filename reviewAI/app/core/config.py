from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    APP_NAME : str = "포캐스트 한국어 리뷰 감정 분석기"
    
    # 디버그 모드 활성화
    DEBUG : bool = True
    
    # 모델은 미리 내려받은 로컬 디렉터리에서만 불러옵니다.
    MODEL_PATH : str = "./models/sentiment_model"
    MODEL_NAME: str = "WhitePeak/bert-base-cased-Korean-sentiment"
    
    # 텍스트 토큰화 시 처리할 최대 길이 지정
    MAX_LENGTH: int = 256
    
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        # .env 파일에 정의되지 않은 여분의 값이 있어도 에러 없이 무시하도록 설정합니다.
        extra="ignore"  
    )
    
settings = Settings()

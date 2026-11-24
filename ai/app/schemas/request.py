from pydantic import BaseModel, ConfigDict, Field, field_validator


class PredictRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    text: str = Field(min_length=1, max_length=1000)

    @field_validator("text")
    @classmethod
    def validate_text(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("text는 공백일 수 없습니다.")
        return normalized

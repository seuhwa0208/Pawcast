from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class PredictResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    label: Literal["POSITIVE", "NEGATIVE"]
    label_ko: Literal["긍정", "부정"] = Field(alias="labelKo")
    confidence: float = Field(ge=0, le=1)
    probabilities: dict[str, float]
    model_name: str = Field(alias="modelName")

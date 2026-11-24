from fastapi import APIRouter

from app.schemas.request import PredictRequest
from app.schemas.response import PredictResponse
from app.services.predict_service import predict_sentiment


router = APIRouter(prefix="/predict", tags=["prediction"])


@router.post("", response_model=PredictResponse)
def predict(request: PredictRequest) -> PredictResponse:
    return predict_sentiment(request)

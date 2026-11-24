from app.core.config import settings
from app.core.model_loader import get_model
from app.schemas.request import PredictRequest
from app.schemas.response import PredictResponse


LABEL_KO = {
    "POSITIVE": "긍정",
    "NEGATIVE": "부정",
}


def predict_sentiment(request: PredictRequest) -> PredictResponse:
    model = get_model()
    probabilities = model.predict_proba([request.text])[0]
    labels = [str(label) for label in model.classes_]
    probability_map = {
        label: round(float(probability), 6)
        for label, probability in zip(labels, probabilities, strict=True)
    }
    label = max(probability_map, key=probability_map.get)
    return PredictResponse(
        label=label,
        labelKo=LABEL_KO[label],
        confidence=probability_map[label],
        probabilities=probability_map,
        modelName=settings.MODEL_NAME,
    )

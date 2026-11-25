"""trained_model.pkl을 기존 리뷰 도메인 계약으로 감싸는 서비스입니다."""

from app.core.config import settings
from app.core.model_loader import get_model


class SentimentAnalyzer:
    def __init__(self) -> None:
        self.model = get_model()
        self.model_name = settings.MODEL_NAME

    def predict(self, text: str) -> dict[str, object]:
        probabilities = self.model.predict_proba([text])[0]
        scores = {
            str(label): float(score)
            for label, score in zip(self.model.classes_, probabilities, strict=True)
        }
        predicted_label = max(scores, key=scores.get)
        return {
            "review_text": text,
            "sentiment": "긍정" if predicted_label == "POSITIVE" else "부정",
            "confidence": scores[predicted_label],
            "negative_score": scores.get("NEGATIVE", 0.0),
            "positive_score": scores.get("POSITIVE", 0.0),
        }


sentiment_analyzer: SentimentAnalyzer | None = None


def get_sentiment_analyzer() -> SentimentAnalyzer:
    global sentiment_analyzer
    if sentiment_analyzer is None:
        sentiment_analyzer = SentimentAnalyzer()
    return sentiment_analyzer

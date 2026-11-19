import unittest
import asyncio
from unittest.mock import AsyncMock, patch

from fastapi import HTTPException

from app.review.api import analyze_review
from app.review.schemas import ReviewRequest


class FakeAnalyzer:
    model_name = "test-model"

    def __init__(self, sentiment: str):
        self.sentiment = sentiment

    def predict(self, text: str) -> dict:
        return {
            "sentiment": self.sentiment,
            "confidence": 0.95,
        }


class ReviewApiTest(unittest.TestCase):
    def test_converts_korean_positive_label(self):
        with patch(
            "app.review.api.get_sentiment_analyzer",
            return_value=FakeAnalyzer("긍정"),
        ), patch(
            "app.review.api.run_in_threadpool",
            new=AsyncMock(return_value={"sentiment": "긍정", "confidence": 0.95}),
        ):
            response = asyncio.run(
                analyze_review(
                    ReviewRequest(content="배송이 빠르고 정말 좋아요.")
                )
            )

        self.assertEqual(response.sentiment, "POSITIVE")

    def test_converts_korean_negative_label(self):
        with patch(
            "app.review.api.get_sentiment_analyzer",
            return_value=FakeAnalyzer("부정"),
        ), patch(
            "app.review.api.run_in_threadpool",
            new=AsyncMock(return_value={"sentiment": "부정", "confidence": 0.95}),
        ):
            response = asyncio.run(
                analyze_review(
                    ReviewRequest(content="크기가 작아서 사용하기 불편해요.")
                )
            )

        self.assertEqual(response.sentiment, "NEGATIVE")

    def test_rejects_profanity_before_model(self):
        with self.assertRaises(HTTPException) as context:
            asyncio.run(
                analyze_review(
                    ReviewRequest(content="시*발 배송이 왜 이래")
                )
            )

        self.assertEqual(context.exception.status_code, 400)
        self.assertEqual(context.exception.detail["code"], "PROHIBITED_WORD")


if __name__ == "__main__":
    unittest.main()

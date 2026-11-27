from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_check() -> None:
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_predict_positive_review() -> None:
    response = client.post("/predict", json={"text": "배송이 빠르고 상품이 정말 좋아요"})
    assert response.status_code == 200
    body = response.json()
    assert body["label"] in {"POSITIVE", "NEGATIVE"}
    assert 0 <= body["confidence"] <= 1
    assert set(body["probabilities"]) == {"POSITIVE", "NEGATIVE"}


def test_reject_blank_text() -> None:
    response = client.post("/predict", json={"text": "   "})
    assert response.status_code == 422

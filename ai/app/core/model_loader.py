import pickle
from functools import lru_cache

from app.core.config import settings


@lru_cache(maxsize=1)
def get_model():
    model_path = settings.MODEL_PATH.expanduser().resolve()
    if not model_path.is_file():
        raise RuntimeError(f"학습 모델을 찾을 수 없습니다: {model_path}")
    with model_path.open("rb") as model_file:
        model = pickle.load(model_file)
    if not hasattr(model, "predict_proba") or not hasattr(model, "classes_"):
        raise RuntimeError("trained_model.pkl이 지원하는 분류 모델이 아닙니다.")
    return model

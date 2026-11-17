"""POWCAST 리뷰 분류 파이프라인 평가 스크립트.

욕설 필터를 먼저 적용하고, 욕설이 아닌 리뷰는 감성 모델로
긍정/부정을 분류해 ABUSIVE/POSITIVE/NEGATIVE 3개 클래스로 평가한다.
"""

import argparse
import json
from pathlib import Path

import pandas as pd
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score

from app.services.profanity_service import contains_prohibited_word
from app.services.sentiment_service import get_sentiment_analyzer


BASE_DIR = Path(__file__).resolve().parent
DEFAULT_CSV_PATH = BASE_DIR / "data" / "product_reviews_5000.csv"
RESULTS_DIR = BASE_DIR / "evaluation_results"
LABELS = ["ABUSIVE", "POSITIVE", "NEGATIVE"]
LABEL_NAMES = {
    "ABUSIVE": "욕설",
    "POSITIVE": "긍정",
    "NEGATIVE": "부정",
}
SENTIMENT_LABEL_MAP = {"긍정": "POSITIVE", "부정": "NEGATIVE"}

# POWCAST 서비스 검증 기준
# 욕설 미탐지는 실제 노출 사고로 이어질 수 있어 재현율 기준을 가장 높게 둔다.
PASS_CRITERIA = {
    "accuracy": 0.80,
    "macro_f1": 0.80,
    "abusive_recall": 0.90,
    "positive_f1": 0.75,
    "negative_f1": 0.75,
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="리뷰 욕설/감성 모델 평가")
    parser.add_argument(
        "--csv",
        type=Path,
        default=DEFAULT_CSV_PATH,
        help=f"평가 CSV 경로 (기본값: {DEFAULT_CSV_PATH})",
    )
    return parser.parse_args()


def load_evaluation_data(csv_path: Path) -> pd.DataFrame:
    resolved_path = csv_path.expanduser().resolve()
    if not resolved_path.is_file():
        raise FileNotFoundError(f"평가 CSV를 찾을 수 없습니다: {resolved_path}")

    # 생성된 CSV는 Excel 한글 호환을 위해 UTF-8 BOM을 사용한다.
    df = pd.read_csv(resolved_path, encoding="utf-8-sig")
    required_columns = {"content", "label"}
    missing_columns = required_columns - set(df.columns)
    if missing_columns:
        raise ValueError(
            "평가 CSV에 필수 컬럼이 없습니다: "
            + ", ".join(sorted(missing_columns))
        )

    df = df.copy()
    df["content"] = df["content"].fillna("").astype(str).str.strip()
    df["label"] = df["label"].fillna("").astype(str).str.strip().str.upper()

    if (df["content"] == "").any():
        empty_count = int((df["content"] == "").sum())
        raise ValueError(f"내용이 비어 있는 리뷰가 {empty_count}건 있습니다.")

    unknown_labels = sorted(set(df["label"]) - set(LABELS))
    if unknown_labels:
        raise ValueError(f"지원하지 않는 라벨이 있습니다: {unknown_labels}")

    return df


def predict_reviews(df: pd.DataFrame) -> list[str]:
    analyzer = get_sentiment_analyzer()
    predictions: list[str] = []
    total = len(df)

    for index, content in enumerate(df["content"], start=1):
        if contains_prohibited_word(content):
            predicted_label = "ABUSIVE"
        else:
            sentiment = analyzer.predict(content)["sentiment"]
            try:
                predicted_label = SENTIMENT_LABEL_MAP[sentiment]
            except KeyError as error:
                raise ValueError(
                    f"감성 모델이 알 수 없는 라벨을 반환했습니다: {sentiment}"
                ) from error

        predictions.append(predicted_label)
        if index % 250 == 0 or index == total:
            print(f"평가 진행: {index:,}/{total:,}")

    return predictions


def evaluate(y_true: list[str], y_pred: list[str]) -> dict:
    """모델 지표를 계산하고 POWCAST 합격 기준 통과 여부를 반환한다."""
    accuracy = accuracy_score(y_true, y_pred)
    macro_f1 = f1_score(y_true, y_pred, labels=LABELS, average="macro")
    class_report = classification_report(
        y_true,
        y_pred,
        labels=LABELS,
        output_dict=True,
        zero_division=0,
    )

    metrics = {
        "accuracy": float(accuracy),
        "macro_f1": float(macro_f1),
        "abusive_recall": float(class_report["ABUSIVE"]["recall"]),
        "positive_f1": float(class_report["POSITIVE"]["f1-score"]),
        "negative_f1": float(class_report["NEGATIVE"]["f1-score"]),
    }
    checks = {
        name: metrics[name] >= minimum
        for name, minimum in PASS_CRITERIA.items()
    }
    return {
        "metrics": metrics,
        "checks": checks,
        "passed": all(checks.values()),
        "class_report": class_report,
    }


def print_evaluation(y_true: list[str], y_pred: list[str], result: dict) -> None:
    """영문 라이브러리 표 대신 프로젝트 관점의 한글 평가표를 출력한다."""
    class_report = result["class_report"]
    rows = []
    for label in LABELS:
        values = class_report[label]
        rows.append(
            {
                "분류": LABEL_NAMES[label],
                "정밀도": values["precision"],
                "재현율": values["recall"],
                "F1 점수": values["f1-score"],
                "실제 건수": int(values["support"]),
            }
        )
    report_df = pd.DataFrame(rows).set_index("분류")

    print("\n===== 리뷰 분류 모델 최종 평가 =====")
    print(f"평가 데이터: {len(y_true):,}건")
    print(f"전체 정확도: {result['metrics']['accuracy']:.4f}")
    print(f"클래스 균형 점수(Macro F1): {result['metrics']['macro_f1']:.4f}\n")
    print("클래스별 평가 결과 (0~1, 높을수록 좋음):")
    print(report_df.to_string(float_format=lambda value: f"{value:.4f}"))

    matrix = confusion_matrix(y_true, y_pred, labels=LABELS)
    korean_labels = [LABEL_NAMES[label] for label in LABELS]
    print("\n혼동 행렬 (행=실제 정답, 열=모델 예측):")
    print(pd.DataFrame(matrix, index=korean_labels, columns=korean_labels).to_string())

    print("\n===== POWCAST 서비스 적용 기준 =====")
    criterion_names = {
        "accuracy": "전체 정확도",
        "macro_f1": "Macro F1",
        "abusive_recall": "욕설 재현율(욕설을 놓치지 않는 비율)",
        "positive_f1": "긍정 F1",
        "negative_f1": "부정 F1",
    }
    for key, minimum in PASS_CRITERIA.items():
        value = result["metrics"][key]
        status = "통과" if result["checks"][key] else "미달"
        print(f"[{status}] {criterion_names[key]}: {value:.4f} / 기준 {minimum:.2f} 이상")
    final_status = "최종 통과" if result["passed"] else "최종 미통과"
    print(f"\n판정: {final_status}")


def save_results(df: pd.DataFrame, y_pred: list[str], result: dict) -> None:
    """전체 예측과 오분류 사례를 재검토할 수 있도록 파일로 남긴다."""
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    evaluated_df = df.copy()
    evaluated_df["predicted_label"] = y_pred
    evaluated_df["is_correct"] = evaluated_df["label"] == evaluated_df["predicted_label"]
    evaluated_df.to_csv(
        RESULTS_DIR / "all_predictions.csv",
        index=False,
        encoding="utf-8-sig",
    )
    evaluated_df.loc[~evaluated_df["is_correct"]].to_csv(
        RESULTS_DIR / "misclassified_reviews.csv",
        index=False,
        encoding="utf-8-sig",
    )

    json_result = {
        "total_reviews": len(evaluated_df),
        "correct_reviews": int(evaluated_df["is_correct"].sum()),
        "incorrect_reviews": int((~evaluated_df["is_correct"]).sum()),
        "pass_criteria": PASS_CRITERIA,
        "metrics": result["metrics"],
        "checks": result["checks"],
        "passed": result["passed"],
    }
    (RESULTS_DIR / "metrics.json").write_text(
        json.dumps(json_result, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"\n상세 결과 저장 위치: {RESULTS_DIR}")


def main() -> None:
    args = parse_args()
    df = load_evaluation_data(args.csv)
    print(f"평가 CSV: {args.csv.expanduser().resolve()}")
    print("정답 분포:")
    print(df["label"].value_counts().reindex(LABELS, fill_value=0).to_string())

    y_true = df["label"].tolist()
    y_pred = predict_reviews(df)
    result = evaluate(y_true, y_pred)
    print_evaluation(y_true, y_pred, result)
    save_results(df, y_pred, result)


if __name__ == "__main__":
    main()

"""AI/data 공식 데이터의 구조, 분할, 참조 무결성을 검증한다."""

import json
from pathlib import Path

import pandas as pd


AI_ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = AI_ROOT / "data"
RESULT_PATH = AI_ROOT / "evaluation_results" / "dataset_validation.json"

DATASETS = {
    "review_sentiment": ("review/ai_review_sentiment.csv", {"content", "label", "split"}),
    "review_gold_candidates": ("review/ai_review_gold_candidates_blank.csv", set()),
    "interactions": ("recommendation/ai_interactions.csv", {"member_id", "product_id", "event", "split"}),
    "products": ("recommendation/ai_products.csv", {"product_id"}),
    "members": ("recommendation/ai_members.csv", {"member_id"}),
    "member_repurchase": ("recommendation/ai_member_repurchase.csv", {"member_id", "split", "target_repurchase_90d"}),
    "sales_daily": ("sales/ai_sales_daily.csv", {"date", "net_sales", "split"}),
    "sales_daily_by_group": ("sales/ai_sales_daily_by_group.csv", {"date", "split"}),
    "calendar_future": ("sales/ai_calendar_future.csv", {"date"}),
}


def load_dataset(name: str, relative_path: str, required_columns: set[str]) -> tuple[pd.DataFrame, dict]:
    path = DATA_ROOT / relative_path
    if not path.is_file():
        raise FileNotFoundError(f"필수 데이터가 없습니다: {path}")
    df = pd.read_csv(path, encoding="utf-8-sig")
    missing = required_columns - set(df.columns)
    if missing:
        raise ValueError(f"{name} 필수 컬럼 누락: {sorted(missing)}")
    summary = {"path": str(path), "rows": len(df), "columns": len(df.columns)}
    if "split" in df.columns:
        summary["splits"] = {
            str(key): int(value) for key, value in df["split"].value_counts().items()
        }
    return df, summary


def main() -> None:
    frames: dict[str, pd.DataFrame] = {}
    result: dict[str, object] = {"datasets": {}}
    for name, (relative_path, required_columns) in DATASETS.items():
        frame, summary = load_dataset(name, relative_path, required_columns)
        frames[name] = frame
        result["datasets"][name] = summary

    review_labels = set(frames["review_sentiment"]["label"].dropna().astype(str))
    if review_labels != {"긍정", "부정"}:
        raise ValueError(f"리뷰 라벨이 긍정/부정 2분류가 아닙니다: {sorted(review_labels)}")

    product_ids = set(frames["products"]["product_id"])
    unknown_products = set(frames["interactions"]["product_id"]) - product_ids
    if unknown_products:
        raise ValueError(f"상품 마스터에 없는 interaction product_id: {len(unknown_products)}건")

    member_ids = set(frames["members"]["member_id"])
    unknown_members = set(frames["interactions"]["member_id"]) - member_ids
    unknown_member_rows = int(
        frames["interactions"]["member_id"].isin(unknown_members).sum()
    )

    result["integrity"] = {
        "review_labels": sorted(review_labels),
        "binary_review_policy": "AI 실사용 CSV에서 중립 행 제거",
        "unknown_interaction_products": 0,
        "unknown_interaction_member_ids": sorted(
            int(member_id) for member_id in unknown_members
        ),
        "unknown_interaction_member_rows": unknown_member_rows,
        "never_train_file": str(DATA_ROOT / "reference" / "DESIGN_TRUTH_do_not_train.csv"),
    }
    RESULT_PATH.parent.mkdir(parents=True, exist_ok=True)
    RESULT_PATH.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    print(f"\n검증 완료: {RESULT_PATH}")


if __name__ == "__main__":
    main()

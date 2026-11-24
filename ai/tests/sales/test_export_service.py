"""매출 통계 파일 생성 기능을 테스트합니다."""

import pandas as pd

from app.sales.export_service import create_csv_file, create_excel_file


def _frames() -> dict[str, pd.DataFrame]:
    return {
        "summary": pd.DataFrame([{"total_sales": 100000, "total_orders": 2}]),
        "categories": pd.DataFrame([{"category_name": "강아지", "sales": 100000}]),
        "daily": pd.DataFrame([{"sale_date": "2026-11-20", "sales": 100000}]),
        "products": pd.DataFrame([{"product_id": 1, "name": "테스트 상품"}]),
        "reviews": pd.DataFrame([{"product_id": 1, "positive_rate": 80.0}]),
        "forecast": pd.DataFrame([{"predicted_sales": 120000, "growth_rate": 20.0}]),
    }


def test_all_csv_is_zip() -> None:
    content, filename, media_type = create_csv_file(_frames(), "all")
    assert content[:2] == b"PK"
    assert filename.endswith(".zip")
    assert media_type == "application/zip"


def test_excel_is_xlsx() -> None:
    content, filename, media_type = create_excel_file(_frames(), "all")
    assert content[:2] == b"PK"
    assert filename.endswith(".xlsx")
    assert "spreadsheetml" in media_type

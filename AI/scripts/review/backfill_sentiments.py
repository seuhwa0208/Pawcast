"""기존 공개 리뷰를 감정 분석하고 review_sentiments에 저장합니다."""

import argparse
import asyncio
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from sqlalchemy import text

from app.common.database import async_session_factory
from app.review.service import analyze_and_store_review


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="기존 리뷰 감정 분석 DB 백필")
    parser.add_argument(
        "--limit",
        type=int,
        default=0,
        help="처리할 최대 리뷰 수, 0이면 전체",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="이미 분석된 리뷰도 다시 분석",
    )
    return parser.parse_args()


async def load_review_ids(limit: int, force: bool) -> list[int]:
    condition = "" if force else "AND rs.review_id IS NULL"
    limit_clause = "LIMIT :limit" if limit > 0 else ""
    statement = text(
        f"""
        SELECT pr.id
        FROM product_reviews pr
        LEFT JOIN review_sentiments rs ON rs.review_id = pr.id
        WHERE pr.is_visible = TRUE
          {condition}
        ORDER BY pr.id
        {limit_clause}
        """
    )
    parameters = {"limit": limit} if limit > 0 else {}
    async with async_session_factory() as session:
        result = await session.execute(statement, parameters)
        return [int(row.id) for row in result]


async def main() -> None:
    args = parse_args()
    review_ids = await load_review_ids(args.limit, args.force)
    total = len(review_ids)
    if total == 0:
        print("새로 분석할 공개 리뷰가 없습니다.")
        return

    print(f"감정 분석 대상: {total:,}건")
    failed: list[tuple[int, str]] = []
    async with async_session_factory() as session:
        for index, review_id in enumerate(review_ids, start=1):
            try:
                await analyze_and_store_review(session, review_id)
            except Exception as error:
                await session.rollback()
                failed.append((review_id, str(error)))
            if index % 50 == 0 or index == total:
                print(f"진행: {index:,}/{total:,} · 실패: {len(failed):,}")

    if failed:
        print("실패한 리뷰 ID:")
        for review_id, message in failed[:20]:
            print(f"- {review_id}: {message}")
        raise SystemExit(1)

    print("기존 리뷰 감정 분석 및 DB 저장 완료")


if __name__ == "__main__":
    asyncio.run(main())

"""리뷰 감정 분석 저장과 베스트 리뷰 상품 조회를 담당합니다."""

from dataclasses import dataclass
from decimal import Decimal

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


@dataclass(frozen=True)
class ReviewData:
    review_id: int
    content: str


async def find_visible_review(
    session: AsyncSession,
    review_id: int,
) -> ReviewData | None:
    result = await session.execute(
        text(
            """
            SELECT id, content
            FROM product_reviews
            WHERE id = :review_id
              AND is_visible = TRUE
            """
        ),
        {"review_id": review_id},
    )
    row = result.mappings().one_or_none()
    if row is None:
        return None
    return ReviewData(review_id=int(row["id"]), content=str(row["content"]))


async def save_sentiment(
    session: AsyncSession,
    review_id: int,
    sentiment: str,
    confidence: Decimal,
    model_name: str,
) -> None:
    await session.execute(
        text(
            """
            INSERT INTO review_sentiments
                (review_id, sentiment, confidence, model_name, analyzed_at)
            VALUES
                (:review_id, :sentiment, :confidence, :model_name, CURRENT_TIMESTAMP)
            ON CONFLICT (review_id) DO UPDATE SET
                sentiment = EXCLUDED.sentiment,
                confidence = EXCLUDED.confidence,
                model_name = EXCLUDED.model_name,
                analyzed_at = CURRENT_TIMESTAMP
            """
        ),
        {
            "review_id": review_id,
            "sentiment": sentiment,
            "confidence": confidence,
            "model_name": model_name,
        },
    )


async def find_best_review_products(
    session: AsyncSession,
    limit: int,
) -> list[dict]:
    """평균 평점 상위 상품에서 정렬 규칙상 첫 리뷰를 조회합니다."""
    result = await session.execute(
        text(
            """
            WITH top_products AS (
                SELECT
                    p.id AS product_id,
                    ROUND(AVG(pr.rating)::numeric, 1) AS average_rating,
                    COUNT(pr.id) AS review_count
                FROM products p
                JOIN product_reviews pr
                  ON pr.product_id = p.id
                 AND pr.is_visible = TRUE
                GROUP BY p.id
                ORDER BY
                    AVG(pr.rating) DESC,
                    COUNT(pr.id) DESC,
                    p.id DESC
                LIMIT :limit
            ), ranked_reviews AS (
                SELECT
                    pr.id AS review_id,
                    pr.product_id,
                    pr.rating,
                    pr.content,
                    m.name AS writer_name,
                    pr.created_at,
                    CASE
                        WHEN rs.sentiment = 'POSITIVE' THEN rs.confidence
                        WHEN rs.sentiment = 'NEGATIVE' THEN 1 - rs.confidence
                        ELSE 0
                    END AS positive_probability,
                    ROW_NUMBER() OVER (
                        PARTITION BY pr.product_id
                        ORDER BY
                            pr.rating DESC,
                            CASE
                                WHEN rs.sentiment = 'POSITIVE' THEN rs.confidence
                                WHEN rs.sentiment = 'NEGATIVE' THEN 1 - rs.confidence
                                ELSE 0
                            END DESC,
                            pr.created_at DESC,
                            pr.id DESC
                    ) AS review_rank
                FROM product_reviews pr
                JOIN top_products tp ON tp.product_id = pr.product_id
                JOIN members m ON m.id = pr.member_id
                LEFT JOIN review_sentiments rs ON rs.review_id = pr.id
                WHERE pr.is_visible = TRUE
            ), representative_variants AS (
                SELECT
                    pv.product_id,
                    pv.id AS variant_id,
                    pv.price,
                    ROW_NUMBER() OVER (
                        PARTITION BY pv.product_id
                        ORDER BY
                            CASE
                                WHEN pv.product_variant_status = 'ON_SALE'
                                 AND pv.stock > 0 THEN 0
                                ELSE 1
                            END,
                            pv.price ASC,
                            pv.id ASC
                    ) AS variant_rank,
                    MAX(
                        CASE
                            WHEN pv.product_variant_status = 'ON_SALE'
                             AND pv.stock > 0 THEN 1
                            ELSE 0
                        END
                    ) OVER (PARTITION BY pv.product_id) AS has_stock
                FROM product_variants pv
                WHERE pv.product_variant_status IN ('ON_SALE', 'SOLD_OUT')
            )
            SELECT
                p.id,
                p.name,
                p.brand,
                p.product_type,
                c.name AS category_name,
                tp.average_rating,
                tp.review_count,
                p.sales_count,
                p.wish_count,
                rv.variant_id,
                rv.price,
                (rv.has_stock = 0) AS sold_out,
                (
                    SELECT pi.image_path
                    FROM product_images pi
                    WHERE pi.product_id = p.id
                    ORDER BY
                        pi.is_main DESC,
                        pi.sort_order ASC NULLS LAST,
                        pi.id ASC
                    LIMIT 1
                ) AS image_path,
                rr.review_id,
                rr.rating AS best_review_rating,
                rr.content AS best_review_content,
                rr.writer_name,
                rr.created_at AS best_review_created_at,
                ROUND(rr.positive_probability * 100, 2) AS positive_percentage
            FROM top_products tp
            JOIN products p ON p.id = tp.product_id
            LEFT JOIN categories c ON c.id = p.category_id
            JOIN ranked_reviews rr
              ON rr.product_id = p.id
             AND rr.review_rank = 1
            JOIN representative_variants rv
              ON rv.product_id = p.id
             AND rv.variant_rank = 1
            ORDER BY
                tp.average_rating DESC,
                tp.review_count DESC,
                p.id DESC
            """
        ),
        {"limit": limit},
    )
    return [dict(row) for row in result.mappings().all()]


async def find_ranked_product_reviews(
    session: AsyncSession,
    product_id: int,
) -> list[dict]:
    """별점·AI 긍정도·최신 작성일·리뷰 ID 순으로 상품 리뷰를 조회합니다."""
    result = await session.execute(
        text(
            """
            SELECT
                pr.id,
                pr.rating,
                pr.content,
                m.name AS writer_name,
                pr.created_at,
                rs.sentiment,
                ROUND(
                    CASE
                        WHEN rs.sentiment = 'POSITIVE' THEN rs.confidence * 100
                        WHEN rs.sentiment = 'NEGATIVE' THEN (1 - rs.confidence) * 100
                        ELSE 0
                    END,
                    2
                ) AS positive_percentage
            FROM product_reviews pr
            JOIN members m ON m.id = pr.member_id
            LEFT JOIN review_sentiments rs ON rs.review_id = pr.id
            WHERE pr.product_id = :product_id
              AND pr.is_visible = TRUE
            ORDER BY
                pr.rating DESC,
                CASE
                    WHEN rs.sentiment = 'POSITIVE' THEN rs.confidence
                    WHEN rs.sentiment = 'NEGATIVE' THEN 1 - rs.confidence
                    ELSE 0
                END DESC,
                pr.created_at DESC,
                pr.id DESC
            """
        ),
        {"product_id": product_id},
    )
    return [dict(row) for row in result.mappings().all()]


async def summarize_sentiments(
    session: AsyncSession,
    limit: int,
) -> dict:
    """저장된 AI 분석 결과만 집계해 전체 비율과 부정 리뷰 상품을 반환한다."""
    overall_result = await session.execute(
        text(
            """
            SELECT
                COUNT(rs.id) AS analyzed_count,
                COUNT(*) FILTER (WHERE rs.sentiment = 'POSITIVE') AS positive_count,
                COUNT(*) FILTER (WHERE rs.sentiment = 'NEGATIVE') AS negative_count
            FROM product_reviews pr
            JOIN review_sentiments rs ON rs.review_id = pr.id
            WHERE pr.is_visible = TRUE
            """
        )
    )
    overall = dict(overall_result.mappings().one())
    analyzed_count = int(overall["analyzed_count"] or 0)
    positive_count = int(overall["positive_count"] or 0)
    negative_count = int(overall["negative_count"] or 0)

    products_result = await session.execute(
        text(
            """
            SELECT
                p.id AS product_id,
                p.name AS product_name,
                COUNT(rs.id) AS analyzed_count,
                COUNT(*) FILTER (WHERE rs.sentiment = 'NEGATIVE') AS negative_count
            FROM products p
            JOIN product_reviews pr ON pr.product_id = p.id AND pr.is_visible = TRUE
            JOIN review_sentiments rs ON rs.review_id = pr.id
            GROUP BY p.id, p.name
            HAVING COUNT(*) FILTER (WHERE rs.sentiment = 'NEGATIVE') > 0
            ORDER BY
                (COUNT(*) FILTER (WHERE rs.sentiment = 'NEGATIVE'))::numeric / COUNT(rs.id) DESC,
                COUNT(*) FILTER (WHERE rs.sentiment = 'NEGATIVE') DESC,
                p.id ASC
            LIMIT :limit
            """
        ),
        {"limit": limit},
    )
    negative_products = []
    for row in products_result.mappings().all():
        item = dict(row)
        count = int(item["analyzed_count"] or 0)
        negatives = int(item["negative_count"] or 0)
        negative_products.append(
            {
                "productId": int(item["product_id"]),
                "productName": str(item["product_name"]),
                "analyzedCount": count,
                "negativeCount": negatives,
                "negativeRate": round(negatives / count * 100, 1) if count else 0,
            }
        )

    return {
        "analyzedCount": analyzed_count,
        "positiveCount": positive_count,
        "negativeCount": negative_count,
        "positiveRate": round(positive_count / analyzed_count * 100, 1) if analyzed_count else 0,
        "negativeRate": round(negative_count / analyzed_count * 100, 1) if analyzed_count else 0,
        "negativeProducts": negative_products,
    }

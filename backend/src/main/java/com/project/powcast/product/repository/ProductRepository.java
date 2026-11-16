package com.project.powcast.product.repository;

import com.project.powcast.product.entity.Product;
import com.project.powcast.product.repository.ProductListView;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    String SEARCH_BASE = """
        -- 선택 카테고리에 하위 카테고리도 같이 검색되게 하는 기능
        WITH RECURSIVE category_scope AS (
            SELECT c.id
            FROM categories c
            WHERE c.id = CAST(:categoryId AS bigint)

            UNION ALL

            SELECT child.id
            FROM categories child
            JOIN category_scope parent
              ON child.parent_id = parent.id
        ),
        -- 상품별 최저 판매가 옵션 선택 준비
        ranked_variants AS (
            SELECT
                v.id,
                v.product_id,
                v.price,
                v.discount_rate,

                FLOOR(
                    v.price * (1 - v.discount_rate / 100.0)
                ) AS sale_price,

                MAX(
                    CASE
                        WHEN v.product_variant_status = 'ON_SALE'
                             AND v.stock > 0
                        THEN 1
                        ELSE 0
                    END
                ) OVER (
                    PARTITION BY v.product_id
                ) AS has_stock,

                ROW_NUMBER() OVER (
                    PARTITION BY v.product_id
                    ORDER BY
                        FLOOR(
                            v.price * (1 - v.discount_rate / 100.0)
                        ) ASC,
                        v.id ASC
                ) AS rn

            FROM product_variants v

            WHERE v.product_variant_status IN (
                'ON_SALE',
                'SOLD_OUT'
            )
            AND (
                :inStockOnly = false
                OR (
                    v.product_variant_status = 'ON_SALE'
                    AND v.stock > 0
                )
            )
        ),
        -- 상품 목록 정보와 평균 별점 계산
        product_rows AS (
            SELECT
                p.id,
                p.name,
                p.brand,
                p.product_type,
                p.category_id,
                p.created_at,
                p.sales_count,
                p.wish_count,
                p.review_count,

                CASE
                    WHEN p.review_count = 0
                    THEN CAST(0.0 AS numeric)
                    ELSE ROUND(
                        CAST(p.rating_sum AS numeric)
                        / p.review_count,
                        1
                    )
                END AS average_rating,

                c.name AS category_name,
                f.main_ingredient,
                f.food_kind,
                f.food_form,
                f.life_stage,

                v.id AS variant_id,
                v.price AS original_price,
                v.sale_price,
                v.discount_rate,
                (v.has_stock = 0) AS sold_out

            FROM products p

            JOIN ranked_variants v
              ON v.product_id = p.id
             AND v.rn = 1

            LEFT JOIN categories c
              ON c.id = p.category_id

            LEFT JOIN food_details f
              ON f.product_id = p.id
        ),
        -- 검색 조건 적용
        filtered_products AS (
            SELECT pr.*
            FROM product_rows pr

            WHERE (
                CAST(:keyword AS text) IS NULL
                OR pr.name ILIKE CONCAT(
                    '%',
                    CAST(:keyword AS text),
                    '%'
                )
            )
            AND (
                CAST(:productType AS text) IS NULL
                OR pr.product_type = CAST(:productType AS text)
            )
            AND (
                CAST(:categoryId AS bigint) IS NULL
                OR pr.category_id IN (
                    SELECT id FROM category_scope
                )
            )
            AND (
                CAST(:brand AS text) IS NULL
                OR pr.brand = CAST(:brand AS text)
            )
            AND (
                CAST(:ingredient AS text) IS NULL
                OR pr.main_ingredient ILIKE CONCAT(
                    '%',
                    CAST(:ingredient AS text),
                    '%'
                )
            )
            AND (
                CAST(:foodKind AS text) IS NULL
                OR pr.food_kind = CAST(:foodKind AS text)
            )
            AND (
                CAST(:foodForm AS text) IS NULL
                OR pr.food_form = CAST(:foodForm AS text)
            )
            AND (
                CAST(:lifeStage AS text) IS NULL
                OR pr.life_stage = CAST(:lifeStage AS text)
            )
            AND (
                CAST(:minPrice AS numeric) IS NULL
                OR pr.sale_price >= CAST(:minPrice AS numeric)
            )
            AND (
                CAST(:maxPrice AS numeric) IS NULL
                OR pr.sale_price <= CAST(:maxPrice AS numeric)
            )
            AND (
                CAST(:minRating AS numeric) IS NULL
                OR pr.average_rating >= CAST(:minRating AS numeric)
            )
        )
        """;

    // 검색·필터·정렬을 적용한 전체 상품 목록
    @Query(
            value = SEARCH_BASE + """
            SELECT
                fp.id AS "id",
                fp.name AS "name",
                fp.brand AS "brand",
                fp.category_name AS "categoryName",
                fp.variant_id AS "variantId",
                fp.original_price AS "originalPrice",
                fp.sale_price AS "salePrice",
                fp.discount_rate AS "discountRate",
                fp.sales_count AS "salesCount",
                fp.wish_count AS "wishCount",
                fp.review_count AS "reviewCount",
                fp.average_rating AS "averageRating",
                fp.sold_out AS "soldOut",

                (
                    SELECT img.image_path
                    FROM product_images img
                    WHERE img.product_id = fp.id
                    ORDER BY
                        img.is_main DESC,
                        img.sort_order ASC NULLS LAST,
                        img.id ASC
                    LIMIT 1
                ) AS "imagePath"

            FROM filtered_products fp

            ORDER BY
                CASE
                    WHEN :sortType IN ('POPULAR', 'WISH_DESC')
                    THEN fp.wish_count
                END DESC,

                CASE
                    WHEN :sortType = 'PRICE_ASC'
                    THEN fp.sale_price
                END ASC,

                CASE
                    WHEN :sortType = 'PRICE_DESC'
                    THEN fp.sale_price
                END DESC,

                CASE
                    WHEN :sortType = 'DISCOUNT_DESC'
                    THEN fp.discount_rate
                END DESC,

                CASE
                    WHEN :sortType = 'SALES_DESC'
                    THEN fp.sales_count
                END DESC,

                CASE
                    WHEN :sortType = 'REVIEW_DESC'
                    THEN fp.review_count
                END DESC,

                CASE
                    WHEN :sortType = 'RATING_DESC'
                    THEN fp.average_rating
                END DESC,

                fp.created_at DESC,
                fp.id DESC
            """,
            nativeQuery = true
    )
    List<ProductListView> searchProducts(
            @Param("keyword") String keyword,
            @Param("productType") String productType,
            @Param("categoryId") Long categoryId,
            @Param("brand") String brand,
            @Param("ingredient") String ingredient,
            @Param("foodKind") String foodKind,
            @Param("foodForm") String foodForm,
            @Param("lifeStage") String lifeStage,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") BigDecimal minRating,
            @Param("inStockOnly") boolean inStockOnly,
            @Param("sortType") String sortType
    );

    // 공개 상품 상세 조회: 품절 상품도 조회 가능
    @Query("""
        SELECT p
        FROM Product p
        LEFT JOIN FETCH p.category
        WHERE p.id = :id
          AND EXISTS (
              SELECT v.id
              FROM ProductVariant v
              WHERE v.product = p
                AND v.productVariantStatus IN (
                    com.project.powcast.product.entity.ProductVariantStatus.ON_SALE,
                    com.project.powcast.product.entity.ProductVariantStatus.SOLD_OUT
                )
          )
        """)
    Optional<Product> findPublicProductById(
            @Param("id") Long id
    );

    // 관리자용 상품 상세 조회
    @Query("""
        SELECT p
        FROM Product p
        LEFT JOIN FETCH p.category
        WHERE p.id = :id
        """)
    Optional<Product> findManagementProductById(
            @Param("id") Long id
    );

    // 브랜드 필터 선택 목록
    @Query("""
        SELECT DISTINCT p.brand
        FROM Product p
        WHERE p.brand IS NOT NULL
          AND TRIM(p.brand) <> ''
        ORDER BY p.brand ASC
        """)
    List<String> findBrands();

    // 판매량 증가
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.salesCount = p.salesCount + :quantity
        WHERE p.id = :productId
          AND :quantity > 0
        """)
    int increaseSalesCount(
            @Param("productId") Long productId,
            @Param("quantity") long quantity
    );

    // 판매량 감소
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.salesCount = p.salesCount - :quantity
        WHERE p.id = :productId
          AND :quantity > 0
          AND p.salesCount >= :quantity
        """)
    int decreaseSalesCount(
            @Param("productId") Long productId,
            @Param("quantity") long quantity
    );

    // 찜 개수 증가
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.wishCount = p.wishCount + 1
        WHERE p.id = :productId
        """)
    int increaseWishCount(
            @Param("productId") Long productId
    );

    // 찜 개수 감소
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.wishCount = p.wishCount - 1
        WHERE p.id = :productId
          AND p.wishCount > 0
        """)
    int decreaseWishCount(
            @Param("productId") Long productId
    );

    // 리뷰 등록
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.reviewCount = p.reviewCount + 1,
            p.ratingSum = p.ratingSum + :rating
        WHERE p.id = :productId
          AND :rating BETWEEN 1 AND 5
        """)
    int addReviewStatistics(
            @Param("productId") Long productId,
            @Param("rating") int rating
    );

    // 리뷰 별점 수정
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.ratingSum = p.ratingSum - :oldRating + :newRating
        WHERE p.id = :productId
          AND p.reviewCount > 0
          AND p.ratingSum >= :oldRating
          AND :oldRating BETWEEN 1 AND 5
          AND :newRating BETWEEN 1 AND 5
        """)
    int updateReviewStatistics(
            @Param("productId") Long productId,
            @Param("oldRating") int oldRating,
            @Param("newRating") int newRating
    );

    // 리뷰 삭제
    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE Product p
        SET p.reviewCount = p.reviewCount - 1,
            p.ratingSum = p.ratingSum - :rating
        WHERE p.id = :productId
          AND p.reviewCount > 0
          AND p.ratingSum >= :rating
          AND :rating BETWEEN 1 AND 5
        """)
    int removeReviewStatistics(
            @Param("productId") Long productId,
            @Param("rating") int rating
    );
}
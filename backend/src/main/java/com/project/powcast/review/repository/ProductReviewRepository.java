package com.project.powcast.review.repository;
import com.project.powcast.product.entity.Product;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ProductReviewRepository extends Repository<Product,Long> {
    @Query(value="""
        SELECT r.id AS "id", r.product_id AS "productId", r.rating AS "rating", r.content AS "content", m.name AS "writerName", r.created_at AS "createdAt"
        FROM product_reviews r JOIN members m ON m.id = r.member_id
        WHERE r.product_id = :productId AND r.is_visible = true AND r.deleted_at IS NULL
        ORDER BY r.created_at DESC, r.id DESC
        """,nativeQuery=true)
    List<ProductReviewView> findVisibleReviews(@Param("productId") Long productId);
    @Query(value="""
        SELECT ranked.id AS "id", ranked.product_id AS "productId", ranked.rating AS "rating", ranked.content AS "content", ranked.writer_name AS "writerName", ranked.created_at AS "createdAt"
        FROM (
            SELECT r.*, m.name AS writer_name,
                ROW_NUMBER() OVER (PARTITION BY r.product_id ORDER BY r.rating DESC, r.created_at DESC, r.id DESC) AS rn
            FROM product_reviews r JOIN members m ON m.id = r.member_id
            WHERE r.product_id IN (:productIds) AND r.is_visible = true AND r.deleted_at IS NULL
        ) ranked WHERE ranked.rn = 1
        """,nativeQuery=true)
    List<ProductReviewView> findBestReviews(@Param("productIds") List<Long> productIds);
}

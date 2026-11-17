package com.project.powcast.review.repository;

import com.project.powcast.review.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.List;

@Repository
public interface ProductReviewRepository
        extends JpaRepository<ProductReview, Long> {

    // 상품별 공개 리뷰 조회
    @Query("""
        SELECT r
        FROM ProductReview r
        WHERE r.product.id = :productId
          AND r.isVisible = true
        ORDER BY r.createdAt DESC, r.id DESC
        """)
    List<ProductReview> findVisibleReviews(
            @Param("productId") Long productId
    );

    // 회원이 작성한 리뷰 조회
    List<ProductReview> findAllByMemberIdOrderByCreatedAtDescIdDesc(
            Long memberId
    );

    // 주문 항목에 이미 리뷰가 있는지 확인
    boolean existsByOrderItemId(Long orderItemId);

    // 상품별 대표 리뷰 조회
    @Query("""
    SELECT r
    FROM ProductReview r
    JOIN FETCH r.member
    WHERE r.product.id IN :productIds
      AND r.isVisible = true
      AND NOT EXISTS (
          SELECT other.id
          FROM ProductReview other
          WHERE other.product = r.product
            AND other.isVisible = true
            AND (
                other.rating > r.rating
                OR (
                    other.rating = r.rating
                    AND other.createdAt > r.createdAt
                )
                OR (
                    other.rating = r.rating
                    AND other.createdAt = r.createdAt
                    AND other.id > r.id
                )
            )
      )
    """)
    List<ProductReview> findBestReviews(
            @Param("productIds") List<Long> productIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ProductReview r WHERE r.id = :id")
    Optional<ProductReview> findByIdForUpdate(@Param("id") Long id);
}
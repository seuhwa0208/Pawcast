package com.project.pawcast.review.repository;

import com.project.pawcast.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository
        extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findAllByReviewIdOrderBySortOrderAscIdAsc(
            Long reviewId
    );

    void deleteAllByReviewId(Long reviewId);
}
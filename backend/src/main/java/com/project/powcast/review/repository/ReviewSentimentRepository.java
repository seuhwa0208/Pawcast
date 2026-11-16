package com.project.powcast.review.repository;

import com.project.powcast.review.entity.ReviewSentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewSentimentRepository
        extends JpaRepository<ReviewSentiment, Long> {

    Optional<ReviewSentiment> findByReviewId(Long reviewId);

    void deleteByReviewId(Long reviewId);
}
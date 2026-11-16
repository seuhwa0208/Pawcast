package com.project.powcast.review.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.product.repository.ProductRepository;
import com.project.powcast.review.dto.BestReviewResponse;
import com.project.powcast.review.dto.ProductReviewsResponse;
import com.project.powcast.review.repository.ProductReviewRepository;
import com.project.powcast.review.repository.ProductReviewView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public Map<Long, BestReviewResponse> findBestReviews(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.findBestReviews(productIds).stream()
                .collect(Collectors.toMap(ProductReviewView::getProductId, BestReviewResponse::from));
    }

    public ProductReviewsResponse getReviews(Long productId) {
        productRepository.findPublicProductById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT));
        List<BestReviewResponse> reviews = reviewRepository.findVisibleReviews(productId).stream()
                .map(BestReviewResponse::from)
                .toList();
        BigDecimal average = reviews.isEmpty()
                ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(reviews.stream().mapToLong(BestReviewResponse::rating).sum())
                        .divide(BigDecimal.valueOf(reviews.size()), 1, RoundingMode.HALF_UP);
        return new ProductReviewsResponse(reviews.size(), average, reviews);
    }
}

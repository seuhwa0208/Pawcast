package com.project.powcast.product.dto;
import com.project.powcast.product.repository.ProductReviewView;
import java.time.LocalDateTime;
public record BestReviewResponse(Long id, int rating, String content, String writerName, LocalDateTime createdAt) {
    public static BestReviewResponse from(ProductReviewView r) {
        return new BestReviewResponse(r.getId(),r.getRating(),r.getContent(),r.getWriterName(),r.getCreatedAt());
    }
}

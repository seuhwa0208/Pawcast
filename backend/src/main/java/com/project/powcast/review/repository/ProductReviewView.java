package com.project.powcast.review.repository;
import java.time.LocalDateTime;
public interface ProductReviewView {
    Long getId();
    Long getProductId();
    int getRating();
    String getContent();
    String getWriterName();
    LocalDateTime getCreatedAt();
}

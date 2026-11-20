package com.project.pawcast.product.dto;

import com.project.pawcast.product.repository.ProductListView;
import com.project.pawcast.review.dto.BestReviewResponse;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String brand,
        String categoryName,
        Long categoryId,
        String productType,
        String mainIngredient,
        String lifeStage,
        String imagePath,
        Long variantId,
        BigDecimal price,
        Long salesCount,
        Long wishCount,
        Long reviewCount,
        BigDecimal averageRating,
        Boolean soldOut,
        BestReviewResponse bestReview
) {
    public static ProductResponse from(ProductListView product) {
        return from(product, null);
    }

    public static ProductResponse from(ProductListView product, BestReviewResponse bestReview) {
        return new ProductResponse(
                product.getId(), product.getName(), product.getBrand(), product.getCategoryName(),
                product.getCategoryId(), product.getProductType(), product.getMainIngredient(), product.getLifeStage(),
                product.getImagePath(), product.getVariantId(), product.getPrice(),
                product.getSalesCount(),
                product.getWishCount(), product.getReviewCount(), product.getAverageRating(),
                product.getSoldOut(), bestReview
        );
    }
}

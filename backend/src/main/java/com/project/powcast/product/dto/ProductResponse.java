package com.project.powcast.product.dto;

import com.project.powcast.product.repository.ProductListView;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String brand,
        String categoryName,
        String imagePath,
        Long variantId,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        BigDecimal discountRate,
        Long salesCount,
        Long wishCount,
        Long reviewCount,
        BigDecimal averageRating,
        Boolean soldOut
) { public static ProductResponse from(ProductListView product) {
    return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getBrand(),
            product.getCategoryName(),
            product.getImagePath(),
            product.getVariantId(),
            product.getOriginalPrice(),
            product.getSalePrice(),
            product.getDiscountRate(),
            product.getSalesCount(),
            product.getWishCount(),
            product.getReviewCount(),
            product.getAverageRating(),
            product.getSoldOut()
    );
}
}

package com.project.powcast.product.dto;

import com.project.powcast.product.entity.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        String brand,
        ProductType productType,
        String description,
        Long categoryId,
        String categoryName,
        long salesCount,
        long wishCount,
        long reviewCount,
        BigDecimal averageRating,
        List<FoodVariantResponse> foodVariants,
        List<DeviceVariantResponse> deviceVariants,
        List<ImageResponse> images,
        FoodDetailResponse foodDetail,
        DeviceDetailResponse deviceDetail
) {
    public static ProductDetailResponse from(
            Product product,
            List<ProductVariant> variants,
            List<ProductImage> images,
            FoodDetail foodDetail,
            DeviceDetail deviceDetail
    ) {
        boolean isFood = product.getProductType() == ProductType.FOOD;
        boolean isDevice = product.getProductType() == ProductType.DEVICE;

        List<FoodVariantResponse> foodVariants = isFood ? variants.stream()
                .map(FoodVariantResponse::from).toList()
                : List.of();

        List<DeviceVariantResponse> deviceVariants = isDevice ? variants.stream()
                .map(DeviceVariantResponse::from).toList()
                : List.of();

        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getProductType(),
                product.getDescription(),
                product.getCategory() == null
                        ? null
                        : product.getCategory().getId(),
                product.getCategory() == null
                        ? null
                        : product.getCategory().getName(),
                product.getSalesCount(),
                product.getWishCount(),
                product.getReviewCount(),
                calculateAverageRating(product),
                foodVariants,
                deviceVariants,
                images.stream()
                        .map(ImageResponse::from)
                        .toList(),
                isFood ? FoodDetailResponse.from(foodDetail) : null,
                isDevice ? DeviceDetailResponse.from(deviceDetail) : null
        );
    }
    private static BigDecimal calculateAverageRating(Product product) {
        if(product.getReviewCount() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(product.getRatingSum())
                .divide(
                        BigDecimal.valueOf(product.getReviewCount()),
                        1,
                        RoundingMode.HALF_UP
                );
    }
}

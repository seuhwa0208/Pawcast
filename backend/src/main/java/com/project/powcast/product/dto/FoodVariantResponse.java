package com.project.powcast.product.dto;

import com.project.powcast.product.entity.ProductVariant;
import com.project.powcast.product.entity.ProductVariantStatus;

import java.math.BigDecimal;

public record FoodVariantResponse(
        Long id,
        String skuCode,
        String optionKey,
        String optionLabel,
        Integer weightG,
        Integer volumeMl,
        BigDecimal originalPrice,
        BigDecimal discountRate,
        BigDecimal salePrice,
        int stock,
        ProductVariantStatus status
) {
    public static FoodVariantResponse from(ProductVariant variant) {
        return new FoodVariantResponse(
                variant.getId(),
                variant.getSkuCode(),
                variant.getOptionKey(),
                variant.getOptionLabel(),
                variant.getWeightG(),
                variant.getVolumeMl(),
                variant.getPrice(),
                variant.getDiscountRate(),
                variant.getSalePrice(),
                variant.getStock(),
                variant.getProductVariantStatus()
        );
    }
}

package com.project.powcast.product.dto;

import com.project.powcast.product.entity.ProductVariant;
import com.project.powcast.product.entity.ProductVariantStatus;

import java.math.BigDecimal;

public record DeviceVariantResponse(
        Long id,
        String skuCode,
        String optionKey,
        String optionLabel,
        String color,
        Integer capacityMl,
        Integer heightCm,
        BigDecimal originalPrice,
        BigDecimal discountRate,
        BigDecimal salePrice,
        int stock,
        ProductVariantStatus status
) {
    public static DeviceVariantResponse from(ProductVariant variant) {
        return new DeviceVariantResponse(
                variant.getId(),
                variant.getSkuCode(),
                variant.getOptionKey(),
                variant.getOptionLabel(),
                variant.getColor(),
                variant.getCapacityMl(),
                variant.getHeightCm(),
                variant.getPrice(),
                variant.getDiscountRate(),
                variant.getSalePrice(),
                variant.getStock(),
                variant.getProductVariantStatus()
        );
    }
}

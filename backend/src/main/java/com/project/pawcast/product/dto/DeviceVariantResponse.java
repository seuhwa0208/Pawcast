package com.project.pawcast.product.dto;

import com.project.pawcast.product.entity.ProductVariant;
import com.project.pawcast.product.entity.ProductVariantStatus;

import java.math.BigDecimal;

public record DeviceVariantResponse(
        Long id,
        String skuCode,
        String optionKey,
        String optionLabel,
        String color,
        Integer capacityMl,
        Integer heightCm,
        BigDecimal price,
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
                variant.getStock(),
                variant.getProductVariantStatus()
        );
    }
}

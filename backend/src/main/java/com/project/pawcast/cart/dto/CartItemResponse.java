package com.project.pawcast.cart.dto;

import com.project.pawcast.cart.entity.CartItem;
import com.project.pawcast.product.entity.ProductVariantStatus;

import java.math.BigDecimal;

public record CartItemResponse(
        Long itemId,
        Long productId,
        Long variantId,
        String productName,
        String optionLabel,
        String imagePath,
        BigDecimal price,
        int quantity,
        BigDecimal totalPrice,
        int stock,
        ProductVariantStatus status,
        boolean purchasable
) {
    public static CartItemResponse from(CartItem item, String imagePath) {
        var variant = item.getVariant();
        return new CartItemResponse(item.getId(), variant.getProduct().getId(), variant.getId(),
                variant.getProduct().getName(), variant.getOptionLabel(), imagePath,
                variant.getPrice(), item.getQuantity(),
                variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                variant.getStock(), variant.getProductVariantStatus(),
                variant.getProductVariantStatus() == ProductVariantStatus.ON_SALE
                        && variant.getStock() >= item.getQuantity());
    }
}

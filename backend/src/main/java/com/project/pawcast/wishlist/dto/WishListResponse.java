package com.project.pawcast.wishlist.dto;

import com.project.pawcast.product.entity.Product;
import com.project.pawcast.wishlist.entity.WishList;

import java.time.LocalDateTime;

public record WishListResponse(
        Long id,
        Long productId,
        String productName,
        LocalDateTime createdAt
) {
    public static WishListResponse from(WishList wishList) {
        Product product = wishList.getProduct();
        return new WishListResponse(
                wishList.getId(),
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                wishList.getCreatedAt()
        );
    }
}
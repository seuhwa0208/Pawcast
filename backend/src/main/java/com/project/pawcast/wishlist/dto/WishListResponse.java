package com.project.pawcast.wishlist.dto;

import com.project.pawcast.pets.dto.PetsResponse;
import com.project.pawcast.pets.entity.Pets;
import com.project.pawcast.product.entity.Product;
import com.project.pawcast.wishlist.entity.WishList;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WishListResponse(

        Product product,
        LocalDateTime createdAt
) {
    public static WishListResponse from(WishList wishList) {
        return new WishListResponse(
                wishList.getProduct(),
                wishList.getCreatedAt()
        );
    }
}

package com.project.powcast.wishlist.dto;

import com.project.powcast.pets.dto.PetsResponse;
import com.project.powcast.pets.entity.Pets;
import com.project.powcast.product.entity.Product;
import com.project.powcast.wishlist.entity.WishList;

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

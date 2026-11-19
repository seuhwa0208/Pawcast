package com.project.pawcast.product.dto;

import com.project.pawcast.product.entity.ProductImage;

public record ImageResponse(
        Long id,
        String imagePath,
        boolean main,
        int sortOrder
) {
    public static ImageResponse from(ProductImage image) {
        return new ImageResponse(
                image.getId(),
                image.getImagePath(),
                image.isMain(),
                image.getSortOrder()
        );
    }
}

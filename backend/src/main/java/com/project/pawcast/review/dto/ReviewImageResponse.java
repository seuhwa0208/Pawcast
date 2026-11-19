package com.project.pawcast.review.dto;

import com.project.pawcast.review.entity.ReviewImage;

public record ReviewImageResponse(

        Long id,
        String imagePath,
        int sortOrder

) {

    public static ReviewImageResponse from(ReviewImage image) {
        return new ReviewImageResponse(
                image.getId(),
                image.getImagePath(),
                image.getSortOrder()
        );
    }
}
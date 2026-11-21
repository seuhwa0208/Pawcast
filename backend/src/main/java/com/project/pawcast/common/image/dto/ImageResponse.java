package com.project.pawcast.common.image.dto;

public record ImageResponse(
        String originalFilename,
        String storedFilename,
        String imageUrl
) {
}
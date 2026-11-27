package com.project.pawcast.petrecommendation.dto;

import com.project.pawcast.petrecommendation.entity.PetRecommendation;

public record PetRecommendationResponse(
        String petName,
        Long productId,
        String productName,
        Integer recommendationRank
) {
    public static PetRecommendationResponse from(PetRecommendation petRecommendation) {
        return new PetRecommendationResponse(
                petRecommendation.getPet().getName(),
                petRecommendation.getProduct().getId(),
                petRecommendation.getProduct().getName(),
                petRecommendation.getRecommendationRank()
        );
    }
}

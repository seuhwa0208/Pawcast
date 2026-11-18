package com.project.powcast.petrecommendation.dto;

import com.project.powcast.petrecommendation.entity.PetRecommendation;
import com.project.powcast.pets.dto.PetsResponse;
import com.project.powcast.pets.entity.Pets;

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

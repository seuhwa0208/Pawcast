package com.project.powcast.product.dto;

import com.project.powcast.product.entity.FoodDetail;

public record FoodDetailResponse(
        String foodKind,
        String foodForm,
        String mainIngredient,
        String lifeStage,
        String ingredientDescription
) {
    public static FoodDetailResponse from(FoodDetail detail) {
        if (detail == null) {
            return null;
        }
        return new FoodDetailResponse(
                detail.getFoodKind(),
                detail.getFoodForm(),
                detail.getMainIngredient(),
                detail.getLifeStage(),
                detail.getIngredientDescription()
        );
    }
}

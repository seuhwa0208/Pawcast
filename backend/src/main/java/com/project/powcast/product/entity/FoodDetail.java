package com.project.powcast.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FoodDetail {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "food_kind")
    private String foodKind;

    @Column(name = "food_form")
    private String foodForm;

    @Column(name = "main_ingredient")
    private String mainIngredient;

    @Column(name = "life_stage")
    private String lifeStage;

    @Column(name = "ingredient_description", columnDefinition = "text")
    private String ingredientDescription;
}
package com.project.powcast.petrecommendation.entity;

import com.project.powcast.common.entity.BaseTime;
import com.project.powcast.pets.entity.Pets;
import com.project.powcast.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "pet_recommendations")
public class PetRecommendation extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pets pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "recommendation_rank", nullable = false)
    private Integer recommendationRank;

}

package com.project.powcast.product.entity;

import com.project.powcast.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sales_count", nullable = false)
    @Builder.Default
    private long salesCount = 0L;

    @Column(name = "wish_count", nullable = false)
    @Builder.Default
    private long wishCount = 0L;

    @Column(name = "review_count", nullable = false)
    private long reviewCount;

    @Column(name = "rating_sum", nullable = false)
    private long ratingSum;
}

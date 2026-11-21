package com.project.pawcast.product.entity;

import com.project.pawcast.common.entity.BaseTime;
import com.project.pawcast.product.entity.Category;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "products")
@DynamicUpdate
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

    @Builder.Default
    @Column(name = "review_count", nullable = false)
    private long reviewCount = 0L;

    @Builder.Default
    @Column(name = "rating_sum", nullable = false)
    private long ratingSum = 0L;
}

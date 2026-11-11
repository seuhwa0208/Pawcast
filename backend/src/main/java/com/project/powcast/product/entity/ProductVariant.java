package com.project.powcast.product.entity;

import com.project.powcast.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku_code", nullable = false, unique = true)
    private String skuCode;

    @Column(name = "option_key", nullable = false, length = 150)
    private String optionKey;

    @Column(name = "option_label", nullable = false, length = 150)
    private String optionLabel;

    @Column(length = 50)
    private String color;

    @Column(name = "weight_g")
    private int weightG;

    @Column(name = "volume_ml")
    private int volumeMl;

    @Column(name = "capacity_ml")
    private int capacityMl;

    @Column(name = "height_cm")
    private int heightCm;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "cost_price", nullable = false)
    private BigDecimal costPrice;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_variant_status", nullable = false)
    private ProductVariantStatus productVariantStatus;

    @Column(name = "sales_count", nullable = false)
    @Builder.Default
    private long salesCount = 0L;

    public BigDecimal getSalePrice() {
        return price.multiply(BigDecimal.ONE.subtract(
                discountRate.movePointLeft(2)
        )).setScale(0, RoundingMode.DOWN);
    }
}

package com.project.pawcast.order.entity;

import com.project.pawcast.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "product_name", nullable = false, length = 50)
    private String productName;

    @Column(name = "option_label", nullable = false, length = 50)
    private String optionLabel;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(precision = 12, scale = 0, nullable = false, name = "unit_price")
    private BigDecimal unitPrice;

    @Column(precision = 12, scale = 0, nullable = false, name = "total_price")
    private BigDecimal totalPrice;
}

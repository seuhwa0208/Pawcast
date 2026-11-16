package com.project.powcast.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "device_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceDetail {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "device_kind", nullable = false)
    private String deviceKind;

    private String material;

    @Column(name = "feature_description", columnDefinition = "TEXT")
    private String featureDescription;
}

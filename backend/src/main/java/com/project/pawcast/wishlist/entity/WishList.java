package com.project.pawcast.wishlist.entity;

import com.project.pawcast.member.entity.Members;
import com.project.pawcast.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "wishlists")
public class WishList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Members member;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
}

package com.project.powcast.product.entity;
import com.project.powcast.member.entity.Members;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name="inventory_transactions")
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InventoryTransaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id", nullable=false) private ProductVariant variant;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="member_id") private Members member;
    @Column(name="transaction_type", nullable=false) private String transactionType;
    @Column(name="quantity_change", nullable=false) private int quantityChange;
    @Column(name="stock_before", nullable=false) private int stockBefore;
    @Column(name="stock_after", nullable=false) private int stockAfter;
    @Column(name="reference_type", nullable=false) private String referenceType;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
}

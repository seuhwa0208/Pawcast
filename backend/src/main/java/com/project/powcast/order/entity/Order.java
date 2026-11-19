package com.project.powcast.order.entity;

import com.project.powcast.member.entity.Members;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Members member;

    @Column(unique = true, nullable = false,name = "order_number", length = 22)
    private String orderNumber;

    //배송상태 인듯 주문 상태
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 12, scale = 0, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 0, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "shipping_fee", precision = 12, scale = 0, nullable = false)
    private BigDecimal shippingFee;

    @Column(name = "final_amount", precision = 12, scale = 0, nullable = false)
    private BigDecimal finalAmount;

    @Column(name = "recipient_name", length = 50, nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "recipient_email", length = 100, nullable = false)
    private String recipientEmail;

    @Column(name = "delivery_zipcode", length = 5, nullable = false)
    private String deliveryZipcode;

    @Column(name = "delivery_address1", length = 255, nullable = false)
    private String deliveryAddress1;

    @Column(name = "delivery_address2", length = 255)
    private String deliveryAddress2;

    @Column(name = "delivery_message", length = 255)
    private String deliveryMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

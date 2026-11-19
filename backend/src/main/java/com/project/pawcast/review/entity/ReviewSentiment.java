package com.project.pawcast.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_sentiments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSentiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private ProductReview review;

    @Column(name = "sentiment")
    private String sentiment;

    @Column(name = "confidence")
    private BigDecimal confidence;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
}

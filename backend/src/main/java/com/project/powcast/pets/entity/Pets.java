package com.project.powcast.pets.entity;

import com.project.powcast.common.entity.BaseTime;
import com.project.powcast.member.entity.Members;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "pets")
public class Pets extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Members member;

    @Column(nullable = false, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetType petType;

    private LocalDate birthday;

    @Column(name = "breed_name", length = 50)
    private String breedName;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    public enum PetType {
        CAT,
        DOG
    }
}

package com.project.powcast.address.entity;

import com.project.powcast.member.entity.Members;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "address")
public class Addresses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false, name = "member_id")
    private Members member;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false, length = 50)
    private String recipient;

    private String tel;

    @Column(nullable = false, length = 5)
    private String zipcode;

    @Column(nullable = false, length = 200)
    private String address1;

    @Column(nullable = false, length = 200)
    private String address2;

    private boolean isDefault;
}

package com.project.powcast.outhaccount.entity;

import com.project.powcast.member.entity.Members;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "outh_accounts")
public class OuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Members member;

    // 제공자
    @Column(nullable = false)
    private String provider;

    //제공자 측 사용자 식별자
    @Column(nullable = false)
    private String provider_user_id;
}

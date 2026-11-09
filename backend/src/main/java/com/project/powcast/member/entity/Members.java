package com.project.powcast.member.entity;

import com.project.powcast.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "members")
public class Members extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(length = 50)
    private String tel;

    @Column(nullable = false, length = 300)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberRole role = MemberRole.USER;

    @Column(nullable = false, name = "member_status")
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private MemberStatus memberStatus = MemberStatus.ACTIVE;

    @Column(nullable = false, name = "is_agreed_terms")
    private boolean isAgreedTerms;

    @Column(nullable = false, name = "is_agreed_privacy")
    private boolean isAgreedPrivacy;

    @Column(nullable = false, name = "is_agreed_efinance")
    private boolean isAgreedEfinance;

    @Column(nullable = false, name = "is_agreed_marketing")
    private boolean isAgreedMarketing;
}

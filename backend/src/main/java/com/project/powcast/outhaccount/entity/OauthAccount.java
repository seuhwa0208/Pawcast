package com.project.powcast.outhaccount.entity;

import com.project.powcast.member.entity.Members;
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
@Table(name = "oauth_accounts")
public class OauthAccount {

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
    @Column(nullable = false, name = "provider_user_id" )
    private String providerUserId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

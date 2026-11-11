package com.project.powcast.auth.entity;

import com.project.powcast.member.entity.Members;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "refresh_tokens")
public class RefreshTokens {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Members member;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

//    @Column(name = "user_agent", length = 300)
//    private String userAgent;
//    @Column(name = "ip_address", length = 45)
//    private String ipAddress;

}

package com.project.pawcast.auth.repository;

import com.project.pawcast.auth.entity.RefreshTokens;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokens, Long> {
    Optional<RefreshTokens> findByToken(String token);

    // 같은 refresh 토큰의 동시 재발급을 직렬화해 중복 회전을 막습니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshTokens rt JOIN FETCH rt.member WHERE rt.token = :token")
    Optional<RefreshTokens> findByTokenForUpdate(@Param("token") String token);

    /** 로그아웃·비밀번호 변경·탈퇴 시 그 회원의 모든 토큰을 끊는다. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokens t SET t.revoked = true " + "WHERE t.member.id = :memberId AND t.revoked = false")
    int revokeAllByMember(@Param("memberId") Long memberId);

    /** 만료된 토큰 정리 배치. 쌓아 두면 테이블만 커진다. */
    @Modifying
    @Query("DELETE FROM RefreshTokens t WHERE t.expiresAt < :threshold")
    int deleteExpired(@Param("threshold") OffsetDateTime threshold);

    long countByMemberIdAndRevokedFalse(Long memberId);


}


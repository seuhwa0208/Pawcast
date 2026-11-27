package com.project.pawcast.outhaccount.repository;

import com.project.pawcast.outhaccount.entity.OauthAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT account FROM OauthAccount account JOIN FETCH account.member
            WHERE account.provider = :provider AND account.providerUserId = :providerUserId
            """)
    Optional<OauthAccount> findForUpdate(@Param("provider") String provider,
                                         @Param("providerUserId") String providerUserId);
}
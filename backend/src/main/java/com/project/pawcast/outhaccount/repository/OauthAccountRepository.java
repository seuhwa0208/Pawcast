package com.project.pawcast.outhaccount.repository;

import com.project.pawcast.outhaccount.entity.OauthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {
    Optional<OauthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}

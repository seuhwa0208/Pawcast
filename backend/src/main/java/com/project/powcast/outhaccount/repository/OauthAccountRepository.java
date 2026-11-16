package com.project.powcast.outhaccount.repository;

import com.project.powcast.outhaccount.entity.OauthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {
}

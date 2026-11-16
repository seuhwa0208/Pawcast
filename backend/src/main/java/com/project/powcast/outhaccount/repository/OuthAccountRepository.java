package com.project.powcast.outhaccount.repository;

import com.project.powcast.outhaccount.entity.OuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OuthAccountRepository extends JpaRepository<OuthAccount, Long> {
}

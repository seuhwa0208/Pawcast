package com.project.powcast.member.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification>findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<EmailVerification>deleteByEmail(String email);
}

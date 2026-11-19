package com.project.pawcast.auth.repository;

import com.project.pawcast.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification>findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<EmailVerification>deleteByEmail(String email);
}

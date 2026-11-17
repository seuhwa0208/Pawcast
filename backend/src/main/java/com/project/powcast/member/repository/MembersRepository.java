package com.project.powcast.member.repository;

import com.project.powcast.member.entity.Members;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MembersRepository extends JpaRepository<Members, Long> {
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
    Optional<Members> findByUserId(String userId);
    Optional<Members> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Members m WHERE m.id = :id")
    Optional<Members> findByIdForUpdate(@Param("id") Long id);
}

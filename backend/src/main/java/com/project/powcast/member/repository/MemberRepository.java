package com.project.powcast.member.repository;

import com.project.powcast.member.entity.Members;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Members, Long> {
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
}

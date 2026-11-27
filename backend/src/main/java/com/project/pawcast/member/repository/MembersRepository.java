package com.project.pawcast.member.repository;

import com.project.pawcast.member.dto.MemberListResponse;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.pets.entity.Pets;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembersRepository extends JpaRepository<Members, Long> {
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByEmailAndStatusNot(String email, Members.Status status);
    boolean existsByIdAndStatusNot(Long id, Members.Status status);
    Optional<Members> findByEmail(String email);
    // List<Members> findAllByStatusNotOrderByCreatedAtAsc(Members.Status status);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Members m WHERE m.id = :id")
    Optional<Members> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Members m WHERE m.userId = :userId")
    Optional<Members> findByUserIdForUpdate(@Param("userId") String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Members m WHERE m.email = :email")
    Optional<Members> findByEmailForUpdate(@Param("email") String email);

    @Query("""
    SELECT new com.project.pawcast.member.dto.MemberListResponse(
        m.id,
        m.userId,
        m.name,
        m.email,
        m.tel,
        m.role,
        m.status,
        m.isAgreedTerms,
        m.isAgreedEfinance,
        m.isAgreedPrivacy,
        m.isAgreedMarketing,
        m.createdAt,
        m.updatedAt,
        (SELECT COUNT(p)
         FROM Pets p
         WHERE p.member = m
           AND p.petType = com.project.pawcast.pets.entity.Pets.PetType.DOG),
        (SELECT COUNT(p)
         FROM Pets p
         WHERE p.member = m
           AND p.petType = com.project.pawcast.pets.entity.Pets.PetType.CAT)
        ) FROM Members m
        WHERE m.status <> :status
        ORDER BY m.createdAt ASC
    """)
    List<MemberListResponse> findAllWithPetCounts(@Param("status") Members.Status status,
                                                  @Param("dogType") Pets.PetType dogType,
                                                  @Param("catType") Pets.PetType catType);
}
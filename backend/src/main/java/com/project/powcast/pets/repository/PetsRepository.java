package com.project.powcast.pets.repository;

import com.project.powcast.address.entity.Addresses;
import com.project.powcast.pets.entity.Pets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetsRepository extends JpaRepository<Pets, Long> {
    List<Pets> findAllByMember_Id(Long memberId);
    Optional<Pets> findByIdAndMember_Id(Long petsId, Long memberId);
    long countByMember_Id(Long memberId);
}

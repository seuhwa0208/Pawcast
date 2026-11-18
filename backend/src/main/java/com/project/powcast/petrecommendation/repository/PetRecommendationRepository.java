package com.project.powcast.petrecommendation.repository;

import com.project.powcast.petrecommendation.entity.PetRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRecommendationRepository extends JpaRepository<PetRecommendation, Long> {
    List<PetRecommendation> findAllByPet_IdOrderByRecommendationRankAsc(Long petId);
    Optional<PetRecommendation>
}

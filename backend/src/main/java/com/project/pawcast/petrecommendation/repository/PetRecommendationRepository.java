package com.project.pawcast.petrecommendation.repository;

import com.project.pawcast.petrecommendation.entity.PetRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRecommendationRepository extends JpaRepository<PetRecommendation, Long> {
    List<PetRecommendation> findAllByPet_IdOrderByRecommendationRankAsc(Long petId);
}

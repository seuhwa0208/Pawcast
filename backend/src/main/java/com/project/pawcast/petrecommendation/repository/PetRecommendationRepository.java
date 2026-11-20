package com.project.pawcast.petrecommendation.repository;

import com.project.pawcast.petrecommendation.entity.PetRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRecommendationRepository extends JpaRepository<PetRecommendation, Long> {
    List<PetRecommendation> findAllByPet_IdOrderByRecommendationRankAsc(Long petId);
}

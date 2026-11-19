package com.project.pawcast.petrecommendation.controller;

import com.project.pawcast.petrecommendation.dto.PetRecommendationResponse;
import com.project.pawcast.petrecommendation.service.PetRecommendationService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PetRecommendationController {
    private final PetRecommendationService petRecommendationService;

    @GetMapping("/api/pets/{petId}/recommendations")
    public List<PetRecommendationResponse> getRecommendation(@AuthenticationPrincipal LoginMember member, @PathVariable Long petId){
        return petRecommendationService.recommendationList(member.getId(), petId);
    }
}

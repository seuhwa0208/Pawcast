package com.project.pawcast.pets.controller;

import com.project.pawcast.pets.dto.PetsRequest;
import com.project.pawcast.pets.dto.PetsResponse;
import com.project.pawcast.pets.service.PetsService;
import com.project.pawcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/pets")
public class PetsController {
    private final PetsService petsService;

    // 목록 조회
    @GetMapping
    public List<PetsResponse> getPets(@AuthenticationPrincipal LoginMember member){
        return petsService.listPets(member.getId());
    }
    
    // 반려 동물 정보 등록
    @PostMapping
    public PetsResponse createPets(@AuthenticationPrincipal LoginMember member, PetsRequest request){
        return petsService.createPets(member.getId(),request);
    }
    
    // 수정
    @PatchMapping("/{petId}")
    public PetsResponse updatePets(@AuthenticationPrincipal LoginMember member, @PathVariable Long petsId, PetsRequest request){
        return petsService.updatePets(petsId,member.getId(),request);
    }
    
    // 삭제
    @DeleteMapping("/{petId}")
    public void deletePets(@AuthenticationPrincipal LoginMember member, @PathVariable Long petsId){
        petsService.deletePets(petsId, member.getId());
    }
}

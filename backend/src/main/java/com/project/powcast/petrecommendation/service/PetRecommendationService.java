package com.project.powcast.petrecommendation.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.repository.MembersRepository;
import com.project.powcast.petrecommendation.dto.PetRecommendationResponse;
import com.project.powcast.petrecommendation.repository.PetRecommendationRepository;
import com.project.powcast.pets.entity.Pets;
import com.project.powcast.pets.repository.PetsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetRecommendationService {
    private final PetRecommendationRepository petRecommendationRepository;
    private final MembersRepository membersRepository;
    private final PetsRepository petsRepository;
    
    // 펫 상품 목록 조회
    public List<PetRecommendationResponse> recommendationList(Long memberId, Long petId){
        // 회원이 소유한 펫인가?
        // 펫 아이디와 회원 아이디로 펫이 존재하는지 확인
        Pets pet = petsRepository.findByIdAndMember_Id(memberId, petId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PETS));

        return petRecommendationRepository.findAllByPet_IdOrderByRecommendationRankAsc(pet.getId())
                .stream().map(PetRecommendationResponse::from).toList();
    }

}

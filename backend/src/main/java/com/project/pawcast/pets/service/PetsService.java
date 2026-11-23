package com.project.pawcast.pets.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.pets.dto.PetsRequest;
import com.project.pawcast.pets.dto.PetsResponse;
import com.project.pawcast.pets.entity.Pets;
import com.project.pawcast.pets.repository.PetsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetsService {
    private static final int MAX_PET_COUNT=3;
    private final PetsRepository petsRepository;
    private final MembersRepository membersRepository;

    //단순 펫_ID와 멤버_ID로 배송지 조회
    public Pets findByIdAndMemberId(Long petsId, Long memberId){
        return petsRepository.findByIdAndMember_Id(petsId, memberId)
                .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PETS));
    }

    // 목록 조회
    public List<PetsResponse> listPets(Long memberId){
        return petsRepository.findAllByMember_Id(memberId).stream().map(PetsResponse::from).toList();
    }

    // 등록
    @Transactional
    public PetsResponse createPets(Long memberId, PetsRequest request){
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        //펫 3마리 까지 등록
        long petCount = petsRepository.countByMember_Id(memberId);
        if (petCount >= MAX_PET_COUNT) {throw new BusinessException(ErrorCode.PET_LIMIT_EXCEEDED);}

        Pets pets = Pets.builder()
                .member(member)
                .name(request.name())
                .petType(request.petType())
                .breedName(request.breedName())
                .birthday(request.birthday())
                .weightKg(request.weightKg())
                .lifeStage((request.lifeStage()))
                .allergies(request.allergies())
                .build();
        return PetsResponse.from(petsRepository.save(pets));
    }

    // 수정
    @Transactional
    public PetsResponse updatePets(Long petId, Long memberId, PetsRequest request){

        Pets pets = findByIdAndMemberId(petId, memberId);

        pets.setName(request.name());
        pets.setPetType(request.petType());
        pets.setBreedName(request.breedName());
        pets.setBirthday(request.birthday());
        pets.setLifeStage(request.lifeStage());
        pets.setWeightKg(request.weightKg());
        pets.setAllergies(request.allergies());

        return PetsResponse.from(pets);
    }

    // 삭제
    @Transactional
    public void deletePets(Long petsId, Long memberId){
        Pets pets = findByIdAndMemberId(petsId, memberId);
        petsRepository.delete(pets);
    }
}
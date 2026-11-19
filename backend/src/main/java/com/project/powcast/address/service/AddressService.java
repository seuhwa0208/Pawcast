package com.project.powcast.address.service;

import com.project.powcast.address.dto.AddressRequest;
import com.project.powcast.address.dto.AddressResponse;
import com.project.powcast.address.entity.Addresses;
import com.project.powcast.address.repository.AddressRepository;
import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MembersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;
    private final MembersRepository membersRepository;

    //단순 배송지_ID와 멤버_ID로 배송지 조회
    public Addresses findByIdAndMemberId(Long addressId, Long memberId){
        return addressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_ADDRESS));
    }

    // 배송지 조회
    public List<AddressResponse> listAddresses(Long memberId){
       return addressRepository.findAllByMember_Id(memberId).stream().map(AddressResponse::from).toList();
    }

    // 배송지 등록
    @Transactional
    public AddressResponse createAddress(Long memberId, AddressRequest request){
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        Addresses address = Addresses.builder()
                .member(member)
                .label(request.label())
                .recipient(request.recipient())
                .tel(request.tel())
                .zipcode(request.zipcode())
                .address1(request.address1())
                .address2(request.address2())
                .isDefault(request.isDefault())
                .build();
        return AddressResponse.from(addressRepository.save(address));
    }
    
    // 배송지 수정
    // 본인 확인 -
    @Transactional
    public AddressResponse updateAddress(Long addressId, Long memberId, AddressRequest request){

        Addresses addresses = findByIdAndMemberId(addressId, memberId);

        addresses.setLabel(request.label());
        addresses.setRecipient(request.recipient());
        addresses.setTel(request.tel());
        addresses.setZipcode(request.zipcode());
        addresses.setAddress1(request.address1());
        addresses.setAddress2(request.address2());
        addresses.setDefault(request.isDefault());

        return AddressResponse.from(addresses);
    }
    
    // 배송지 삭제
    // 본인 확인
    @Transactional
    public void deleteAddress(Long addressId, Long memberId){
        Addresses addresses = findByIdAndMemberId(addressId, memberId);
        addressRepository.delete(addresses);
    }
}

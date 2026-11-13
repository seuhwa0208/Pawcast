package com.project.powcast.address.service;

import com.project.powcast.address.dto.AddressRequest;
import com.project.powcast.address.dto.AddressResponse;
import com.project.powcast.address.entity.Addresses;
import com.project.powcast.address.repository.AddressRepository;
import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.entity.Members;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;

    //단순 배송지 아이디로 배송지 조회
    public Addresses findById(Long id){
        return addressRepository.findById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_ADDRESS));
    }

    // 배송지 조회
    public List<AddressResponse> listAddresses(Members member){
       return addressRepository.findAllByMember(member).stream().map(AddressResponse::from).toList();
    }
    // 배송지 등록
    public AddressResponse createAddress(AddressRequest request){
        Addresses address = new Addresses().builder()
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
    public AddressResponse updateAddress(Long id, AddressRequest request){
        Addresses addresses = findById(id);
        addresses.setLabel(request.label());
        addresses.setRecipient(request.recipient());
        addresses.setTel(request.tel());
        addresses.setZipcode(request.zipcode());
        addresses.setAddress1(request.address1());
        addresses.setAddress2(request.address2());
        addresses.setDefault(request.isDefault());

        return AddressResponse.from(addressRepository.save(addresses));
    }
    
    // 배송지 삭제
    public void deleteAddress(Long id,)
}

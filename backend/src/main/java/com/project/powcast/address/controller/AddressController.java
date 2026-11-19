package com.project.powcast.address.controller;

import com.project.powcast.address.dto.AddressRequest;
import com.project.powcast.address.dto.AddressResponse;
import com.project.powcast.address.service.AddressService;
import com.project.powcast.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/addresses")
public class AddressController {

    private final AddressService addressService;
    
    //배송지 조회
    @GetMapping
    public List<AddressResponse> getAddresses(@AuthenticationPrincipal LoginMember member){
        return addressService.listAddresses(member.getId());
    }

    //배송지 등록
    @PostMapping
    public AddressResponse createAddress(@AuthenticationPrincipal LoginMember member, AddressRequest request){
        return addressService.createAddress(member.getId(),request);
    }
    
    //배송지 수정
    @PatchMapping("/{addressId}")
    public AddressResponse updateAddress(@AuthenticationPrincipal LoginMember member,@PathVariable Long addressId, AddressRequest request){
        return addressService.updateAddress(addressId,member.getId(),request);
    }
    
    //배송지 삭제
    @DeleteMapping("/{addressId}")
    public void deleteAddress(@AuthenticationPrincipal LoginMember member, @PathVariable Long addressId){
        addressService.deleteAddress(addressId, member.getId());
    }
}

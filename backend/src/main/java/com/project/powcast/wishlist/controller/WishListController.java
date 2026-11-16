package com.project.powcast.wishlist.controller;

import com.project.powcast.pets.dto.PetsRequest;
import com.project.powcast.pets.dto.PetsResponse;
import com.project.powcast.security.LoginMember;
import com.project.powcast.wishlist.dto.WishListResponse;
import com.project.powcast.wishlist.service.WishListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/wishlist")
public class WishListController {

    private final WishListService wishListService;
    // 목록 조회
    @GetMapping
    public List<WishListResponse> getWishList(@AuthenticationPrincipal LoginMember member){
        return wishListService.listWishList(member.getId());
    }
    
    // 찜 추가
    @PutMapping("/{productId}")
    public WishListResponse createWishList(@AuthenticationPrincipal LoginMember member, @PathVariable Long productId){
        return wishListService.createWishList(member.getId(),productId);
    }
    
    // 찜 삭제
    @DeleteMapping("/{productId}")
    public void deleteWishList(@AuthenticationPrincipal LoginMember member, @PathVariable Long wishListId){
        wishListService.deleteWishList(wishListId, member.getId());
    }
}

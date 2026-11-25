package com.project.pawcast.wishlist.controller;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.security.LoginMember;
import com.project.pawcast.wishlist.dto.WishListResponse;
import com.project.pawcast.wishlist.service.WishListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/wishlist")
public class WishListController {

    private final WishListService wishListService;

    private Long requireMemberId(LoginMember member) {
        if (member == null || member.getId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return member.getId();
    }

    // 목록 조회
    @GetMapping
    public List<WishListResponse> getWishList(@AuthenticationPrincipal LoginMember member) {
        return wishListService.listWishList(requireMemberId(member));
    }

    // 찜 추가
    @PutMapping("/{productId}")
    public WishListResponse createWishList(
            @AuthenticationPrincipal LoginMember member,
            @PathVariable Long productId
    ) {
        return wishListService.createWishList(requireMemberId(member), productId);
    }

    // 찜 삭제
    @DeleteMapping("/{productId}")
    public void deleteWishList(
            @AuthenticationPrincipal LoginMember member,
            @PathVariable Long productId
    ) {
        wishListService.deleteWishList(productId, requireMemberId(member));
    }
}
package com.project.pawcast.cart.controller;

import com.project.pawcast.cart.dto.CartAddRequest;
import com.project.pawcast.cart.dto.CartMergeRequest;
import com.project.pawcast.cart.dto.CartQuantityUpdateRequest;
import com.project.pawcast.cart.dto.CartResponse;
import com.project.pawcast.cart.service.CartService;
import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal LoginMember member) {
        return cartService.getCart(memberId(member));
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal LoginMember member,
                                @Valid @RequestBody CartAddRequest request) {
        return cartService.addItem(memberId(member), request);
    }

    @PostMapping("/merge")
    public CartResponse mergeGuestCart(@AuthenticationPrincipal LoginMember member,
                                       @Valid @RequestBody CartMergeRequest request) {
        return cartService.mergeGuestCart(memberId(member), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateQuantity(@AuthenticationPrincipal LoginMember member,
                                       @PathVariable Long itemId,
                                       @Valid @RequestBody CartQuantityUpdateRequest request) {
        return cartService.updateQuantity(memberId(member), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@AuthenticationPrincipal LoginMember member, @PathVariable Long itemId) {
        cartService.deleteItem(memberId(member), itemId);
    }

    private Long memberId(LoginMember member) {
        if (member == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return member.getId();
    }
}
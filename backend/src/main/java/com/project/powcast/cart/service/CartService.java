package com.project.powcast.cart.service;

import com.project.powcast.cart.dto.*;
import com.project.powcast.cart.entity.Cart;
import com.project.powcast.cart.entity.CartItem;
import com.project.powcast.cart.entity.CartStatus;
import com.project.powcast.cart.repository.CartItemRepository;
import com.project.powcast.cart.repository.CartRepository;
import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MemberRepository;
import com.project.powcast.order.entity.OrderItem;
import com.project.powcast.product.entity.ProductVariant;
import com.project.powcast.product.entity.ProductVariantStatus;
import com.project.powcast.product.repository.ProductImageRepository;
import com.project.powcast.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;

    public CartResponse getCart(Long memberId) {
        requireMember(memberId, false);
        return cartRepository.findFirstByMemberIdAndStatusOrderByIdDesc(memberId, CartStatus.ACTIVE)
                .map(this::response)
                .orElseGet(() -> CartResponse.of(null, List.of()));
    }

    @Transactional
    public CartResponse addItem(Long memberId, CartAddRequest request) {
        requireQuantity(request == null ? null : request.quantity());
        if (request.variantId() == null || request.variantId() < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        // 회원 행을 먼저 잠가 동일 회원의 생성·추가·수정·삭제·결제 정리를 직렬화합니다.
        Members member = requireMember(memberId, true);
        ProductVariant variant = variantRepository.findById(request.variantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT));
        requireAvailable(variant, request.quantity());

        Cart cart = cartRepository.findFirstByMemberIdAndStatusOrderByIdDesc(memberId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.saveAndFlush(new Cart(member)));
        CartItem existing = itemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId()).orElse(null);
        if (existing == null) {
            itemRepository.saveAndFlush(new CartItem(cart, variant, request.quantity()));
        } else {
            long quantity = (long) existing.getQuantity() + request.quantity();
            requireAvailable(variant, quantity);
            existing.changeQuantity((int) quantity);
            itemRepository.flush();
        }
        return response(cart);
    }

    @Transactional
    public CartResponse updateQuantity(Long memberId, Long itemId, CartQuantityUpdateRequest request) {
        requireQuantity(request == null ? null : request.quantity());
        requireMember(memberId, true);
        CartItem item = ownedItem(memberId, itemId);
        requireAvailable(item.getVariant(), request.quantity());
        item.changeQuantity(request.quantity());
        itemRepository.flush();
        return response(item.getCart());
    }

    @Transactional
    public void deleteItem(Long memberId, Long itemId) {
        requireMember(memberId, true);
        itemRepository.delete(ownedItem(memberId, itemId));
    }

    // 반드시 결제 성공을 저장하는 트랜잭션 안에서 호출합니다.
    @Transactional(propagation = Propagation.MANDATORY)
    public int removePurchasedItems(Long memberId, List<OrderItem> purchasedItems) {
        if (memberId == null || purchasedItems == null || purchasedItems.isEmpty()) {
            return 0;
        }
        memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        List<Long> variantIds = purchasedItems.stream()
                .map(item -> item.getProductVariant().getId()).distinct().sorted().toList();
        return itemRepository.deletePurchasedItems(memberId, CartStatus.ACTIVE, variantIds);
    }

    private CartItem ownedItem(Long memberId, Long itemId) {
        if (itemId == null || itemId < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return itemRepository.findOwnedItem(itemId, memberId, CartStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CART_ITEM));
    }

    private Members requireMember(Long memberId, boolean lock) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        Members member = (lock ? memberRepository.findByIdForUpdate(memberId) : memberRepository.findById(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        if (member.getStatus() != Members.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return member;
    }

    private void requireQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_CART_QUANTITY);
        }
    }

    private void requireAvailable(ProductVariant variant, long quantity) {
        if (variant.getProductVariantStatus() != ProductVariantStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
        if (quantity > variant.getStock() || quantity > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private CartResponse response(Cart cart) {
        List<CartItem> items = itemRepository.findAllWithProductByCartId(cart.getId());
        if (items.isEmpty()) {
            return CartResponse.of(cart.getId(), List.of());
        }
        List<Long> productIds = items.stream().map(item -> item.getVariant().getProduct().getId())
                .distinct().toList();
        Map<Long, String> images = new HashMap<>();
        imageRepository.findAllByProductIdInOrderByMainDescSortOrderAscIdAsc(productIds)
                .forEach(image -> images.putIfAbsent(image.getProduct().getId(), image.getImagePath()));
        return CartResponse.of(cart.getId(), items.stream()
                .map(item -> CartItemResponse.from(item, images.get(item.getVariant().getProduct().getId())))
                .toList());
    }
}

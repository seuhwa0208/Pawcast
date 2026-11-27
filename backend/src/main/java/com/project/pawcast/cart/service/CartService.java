package com.project.pawcast.cart.service;

import com.project.pawcast.cart.dto.CartAddRequest;
import com.project.pawcast.cart.dto.CartItemResponse;
import com.project.pawcast.cart.dto.CartMergeRequest;
import com.project.pawcast.cart.dto.CartQuantityUpdateRequest;
import com.project.pawcast.cart.dto.CartResponse;
import com.project.pawcast.cart.entity.Cart;
import com.project.pawcast.cart.entity.CartItem;
import com.project.pawcast.cart.entity.CartStatus;
import com.project.pawcast.cart.repository.CartItemRepository;
import com.project.pawcast.cart.repository.CartMergeRequestRepository;
import com.project.pawcast.cart.repository.CartRepository;
import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.order.entity.OrderItem;
import com.project.pawcast.product.entity.ProductVariant;
import com.project.pawcast.product.entity.ProductVariantStatus;
import com.project.pawcast.product.repository.ProductImageRepository;
import com.project.pawcast.product.repository.ProductVariantRepository;
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
    private final MembersRepository membersRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CartMergeRequestRepository mergeRequestRepository;

    public CartResponse getCart(Long memberId) {
        requireMember(memberId, false);
        return activeCartResponse(memberId);
    }

    @Transactional
    public CartResponse addItem(Long memberId, CartAddRequest request) {
        validateAddRequest(request);
        Members member = requireMember(memberId, true);
        Cart cart = getOrCreateActiveCart(member);

        addOrIncreaseItem(cart, request);
        return response(cart);
    }

    @Transactional
    public CartResponse mergeGuestCart(Long memberId, CartMergeRequest request) {
        if (request == null || request.mergeId() == null
                || request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Members member = requireMember(memberId, true);

        if (!mergeRequestRepository.insertIfAbsent(memberId, request.mergeId())) {
            return activeCartResponse(memberId);
        }

        Cart cart = getOrCreateActiveCart(member);
        for (CartAddRequest item : request.items()) {
            validateAddRequest(item);
            addOrIncreaseItem(cart, item);
        }

        return response(cart);
    }

    @Transactional
    public CartResponse updateQuantity(
            Long memberId, Long itemId, CartQuantityUpdateRequest request
    ) {
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

        membersRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));

        List<Long> variantIds = purchasedItems.stream()
                .map(item -> item.getProductVariant().getId())
                .distinct()
                .sorted()
                .toList();

        return itemRepository.deletePurchasedItems(memberId, CartStatus.ACTIVE, variantIds);
    }

    private CartResponse activeCartResponse(Long memberId) {
        return cartRepository
                .findFirstByMemberIdAndStatusOrderByIdDesc(memberId, CartStatus.ACTIVE)
                .map(this::response)
                .orElseGet(() -> CartResponse.of(null, List.of()));
    }

    private Cart getOrCreateActiveCart(Members member) {
        return cartRepository
                .findFirstByMemberIdAndStatusOrderByIdDesc(
                        member.getId(), CartStatus.ACTIVE
                )
                .orElseGet(() -> cartRepository.saveAndFlush(new Cart(member)));
    }

    private void validateAddRequest(CartAddRequest request) {
        requireQuantity(request == null ? null : request.quantity());
        if (request.variantId() == null || request.variantId() < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void addOrIncreaseItem(Cart cart, CartAddRequest request) {
        ProductVariant variant = variantRepository.findById(request.variantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT));

        CartItem existing = itemRepository
                .findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElse(null);

        long quantity = request.quantity();
        if (existing != null) {
            quantity += existing.getQuantity();
        }

        requireAvailable(variant, quantity);

        if (existing == null) {
            itemRepository.saveAndFlush(new CartItem(cart, variant, (int) quantity));
        } else {
            existing.changeQuantity((int) quantity);
            itemRepository.flush();
        }
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

        Members member = (lock
                ? membersRepository.findByIdForUpdate(memberId)
                : membersRepository.findById(memberId))
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

        List<Long> productIds = items.stream()
                .map(item -> item.getVariant().getProduct().getId())
                .distinct()
                .toList();

        Map<Long, String> images = new HashMap<>();
        imageRepository.findAllByProductIdInOrderByMainDescSortOrderAscIdAsc(productIds)
                .forEach(image ->
                        images.putIfAbsent(image.getProduct().getId(), image.getImagePath())
                );

        return CartResponse.of(cart.getId(), items.stream()
                .map(item -> CartItemResponse.from(
                        item, images.get(item.getVariant().getProduct().getId())
                ))
                .toList());
    }
}
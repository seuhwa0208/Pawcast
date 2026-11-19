package com.project.pawcast.cart.repository;

import com.project.pawcast.cart.entity.CartItem;
import com.project.pawcast.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    @Query("""
        SELECT i FROM CartItem i
        JOIN FETCH i.variant v
        JOIN FETCH v.product
        WHERE i.cart.id = :cartId
        ORDER BY i.id
        """)
    List<CartItem> findAllWithProductByCartId(@Param("cartId") Long cartId);

    @Query("""
        SELECT i FROM CartItem i
        JOIN FETCH i.cart c
        JOIN FETCH i.variant v
        JOIN FETCH v.product
        WHERE i.id = :itemId AND c.member.id = :memberId AND c.status = :status
        """)
    Optional<CartItem> findOwnedItem(@Param("itemId") Long itemId,
                                     @Param("memberId") Long memberId,
                                     @Param("status") CartStatus status);

    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM CartItem i
        WHERE i.cart.id IN (
            SELECT c.id FROM Cart c WHERE c.member.id = :memberId AND c.status = :status
        )
        AND i.variant.id IN :variantIds
        """)
    int deletePurchasedItems(@Param("memberId") Long memberId,
                             @Param("status") CartStatus status,
                             @Param("variantIds") List<Long> variantIds);
}

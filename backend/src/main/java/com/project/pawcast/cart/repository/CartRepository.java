package com.project.pawcast.cart.repository;

import com.project.pawcast.cart.entity.Cart;
import com.project.pawcast.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findFirstByMemberIdAndStatusOrderByIdDesc(Long memberId, CartStatus status);
}

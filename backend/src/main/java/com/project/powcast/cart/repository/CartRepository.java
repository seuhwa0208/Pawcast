package com.project.powcast.cart.repository;

import com.project.powcast.cart.entity.Cart;
import com.project.powcast.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findFirstByMemberIdAndStatusOrderByIdDesc(Long memberId, CartStatus status);
}

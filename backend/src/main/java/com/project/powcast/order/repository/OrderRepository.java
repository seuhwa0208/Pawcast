package com.project.powcast.order.repository;

import com.project.powcast.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByMember_IdOrderByCreatedAtDesc(Long memberId);
    Optional<Order> findByIdAndMemberId(Long orderId, Long memberId);
    Optional<Order> findByEmailAndOrderNumber(String email, String orderNumber);
}

package com.project.pawcast.order.repository;

import com.project.pawcast.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByMember_IdOrderByCreatedAtDesc(Long memberId);
    List<Order> findAllByOrderByCreatedAtDesc();
    Optional<Order> findByIdAndMemberId(Long orderId, Long memberId);
    @Query("SELECT o FROM Order o WHERE o.member IS NULL AND LOWER(o.recipientEmail) = LOWER(:email) AND o.orderNumber = :orderNumber")
    Optional<Order> findGuestOrderByEmailAndOrderNumber(@Param("email") String email,
                                                         @Param("orderNumber") String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}

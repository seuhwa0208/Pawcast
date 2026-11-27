package com.project.pawcast.order.repository;

import com.project.pawcast.order.entity.Order;
import jakarta.persistence.LockModeType;
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
    // 주문번호와 결제 시 사용한 이메일을 모두 아는 사람만 조회할 수 있게 한다.
    // 로그인 주문을 로그아웃한 뒤에도 결제 안내 메일로 주문을 확인할 수 있으므로 member IS NULL 조건은 두지 않는다.
    @Query("SELECT o FROM Order o WHERE LOWER(o.recipientEmail) = LOWER(:email) AND UPPER(o.orderNumber) = UPPER(:orderNumber)")
    Optional<Order> findOrderByEmailAndOrderNumber(@Param("email") String email,
                                                    @Param("orderNumber") String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}

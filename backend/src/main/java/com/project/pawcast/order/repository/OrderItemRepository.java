package com.project.pawcast.order.repository;

import com.project.pawcast.order.entity.OrderItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 특정 주문 ID에 속한 전체 주문 항목 조회
    List<OrderItem> findAllByOrder_Id(Long orderId);

    // 동시성 제어를 위해 비관적 쓰기 락(PESSIMISTIC_WRITE)을 걸고 주문 항목 단건 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM OrderItem i WHERE i.id = :id")
    Optional<OrderItem> findByIdForUpdate(@Param("id") Long id);

    // 여러 주문 ID 목록에 해당하는 주문 항목 일괄 조회
    List<OrderItem> findByOrder_IdIn(List<Long> orderIds);
}
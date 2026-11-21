package com.project.pawcast.refund.repository;

import com.project.pawcast.refund.entity.Refund;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByPaymentId(Long paymentId);
    boolean existsByPaymentId(Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Refund r WHERE r.id = :refundId")
    Optional<Refund> findByIdForUpdate(@Param("refundId") Long refundId);

    @Query("SELECT r FROM Refund r JOIN FETCH r.payment p JOIN FETCH p.order ORDER BY r.createdAt DESC")
    List<Refund> findAllForAdmin();

}

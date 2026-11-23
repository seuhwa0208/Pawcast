package com.project.pawcast.member.admin.repository;

import com.project.pawcast.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminSalesRepository extends JpaRepository<Order, Long> {

    @Query(value = """
            SELECT
               SUM(CASE
                   WHEN p.status = 'PAID'
                    AND o.status != 'CANCELLED'
                   THEN o.final_amount ELSE 0
               END) AS total_sales,

               COUNT(CASE
                   WHEN p.status = 'PAID'
                    AND o.status != 'CANCELLED'
                   THEN o.id
               END) AS order_count,

               AVG(CASE
                   WHEN p.status = 'PAID'
                    AND o.status != 'CANCELLED'
                   THEN o.final_amount
               END) AS average_order_amount,

               SUM(CASE
                   WHEN o.status = 'CANCELLED'
                   THEN o.final_amount ELSE 0
               END) AS cancel_amount
           FROM orders o
           JOIN payments p ON p.order_id = o.id
           WHERE p.paid_at >= :startDate
             AND p.paid_at < :endDate
        """, nativeQuery = true)
    List<Object[]> findSalesStatistics(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

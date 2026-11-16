package com.project.powcast.product.repository;
import com.project.powcast.product.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findAllByVariantProductIdOrderByCreatedAtDescIdDesc(Long productId);
}

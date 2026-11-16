package com.project.powcast.product.repository;
import com.project.powcast.product.entity.ProductVariant;
import com.project.powcast.product.entity.ProductVariantStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findAllByProductIdOrderByIdAsc(Long productId);
    boolean existsBySkuCode(String skuCode);
    boolean existsBySkuCodeAndIdNot(String skuCode, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id IN :ids ORDER BY v.id")
    List<ProductVariant> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.stock = :stock, v.productVariantStatus = :status WHERE v.id = :id")
    int updateStock(@Param("id") Long id, @Param("stock") int stock, @Param("status") ProductVariantStatus status);
}

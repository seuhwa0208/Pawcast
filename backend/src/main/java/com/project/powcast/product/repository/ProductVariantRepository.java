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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT v
    FROM ProductVariant v
    WHERE v.product.id = :productId
    ORDER BY v.id
    """)
    List<ProductVariant> findAllByProductIdForUpdate(
            @Param("productId") Long productId
    );
    // 재고 감소 - 서브쿼리를 써야하는 이유 - 동시에 감소하는 경우가 있을 수있음 - 쿼리는 update문에서 검사하고 감소하기때문에 더 안전함 
    @Modifying
    @Query("""
    UPDATE ProductVariant p
    SET p.stock = p.stock - :quantity
    WHERE p.id = :productVariantId
    AND p.stock >= :quantity
    """)
    int decreaseStock(@Param("productVariantId") Long productVariantId, @Param("quantity") Integer quantity);
    // 재고 증가
    @Modifying
    @Query("""
    UPDATE ProductVariant p
    SET p.stock = p.stock + :quantity
    WHERE p.id = :productVariantId
    """)
    int increaseStock( @Param("productVariantId") Long productVariantId, @Param("quantity") Integer quantity);
}

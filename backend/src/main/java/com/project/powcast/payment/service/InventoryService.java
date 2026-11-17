package com.project.powcast.payment.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductVariantRepository productVariantRepository;

    @Transactional
    public void decreaseStock(Long variantId, Integer quantity) {
        validateRequest(variantId, quantity);

        int updated = productVariantRepository.decreaseStock(
                variantId, quantity, LocalDateTime.now()
        );

        if (updated == 0) {
            validateVariantExists(variantId);
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    @Transactional
    public void increaseStock(Long variantId, Integer quantity) {
        validateRequest(variantId, quantity);

        int updated = productVariantRepository.increaseStock(
                variantId, quantity, LocalDateTime.now()
        );

        if (updated == 0) {
            validateVariantExists(variantId);
            throw new BusinessException(ErrorCode.INVALID_STOCK_ADJUSTMENT);
        }
    }

    private void validateRequest(Long variantId, Integer quantity) {
        if (variantId == null || variantId < 1
                || quantity == null || quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateVariantExists(Long variantId) {
        if (!productVariantRepository.existsById(variantId)) {
            throw new BusinessException(
                    ErrorCode.NOT_FOUND_PRODUCT_VARIANT
            );
        }
    }
}
    /*

@Modifying
@Query("""
    UPDATE ProductVariant p
    SET p.stock = p.stock - :quantity
    WHERE p.id = :productVariantId
    AND p.stock >= :quantity
""")
int decreaseStock(@Param("productVariantId") Long productVariantId, @Param("quantity") Integer quantity);

@Modifying
@Query("""
    UPDATE ProductVariant p
    SET p.stock = p.stock + :quantity
    WHERE p.id = :productVariantId
""")
int increaseStock( @Param("productVariantId") Long productVariantId, @Param("quantity") Integer quantity);

@Transactional
public void decreaseStock(Long productVariantId, Integer quantity) {
    int updated = productVariantRepository.decreaseStock(productVariantId, quantity);
    if (updated == 0) {throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);}
}

@Transactional
public void increaseStock(Long productVariantId, Integer quantity) {
    int updated = productVariantRepository.increaseStock(productVariantId, quantity);
    if (updated == 0) {throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);}
}

 */

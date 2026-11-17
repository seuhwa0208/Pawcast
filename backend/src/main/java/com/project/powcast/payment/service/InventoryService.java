package com.project.powcast.payment.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.product.entity.ProductVariant;
import com.project.powcast.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {
    // Product에 들어갈 기능이지만 일단은 구현 여부를 몰라 구현해둠
    private final ProductVariantRepository productVariantRepository;

    @Transactional
    public void  decreaseStock(Long productVariantId, Integer quantity){
        // 상품 세부 속성 검사
        ProductVariant productVariant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT));
        // 재고 수량 검사
        if (productVariant.getStock() < quantity) { throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);}
        // 재고 저장
        productVariant.setStock(productVariant.getStock() - quantity);
    }

    public void increaseStock(Long productVariantId, Integer quantity){
        ProductVariant productVariant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT));

        productVariant.setStock(productVariant.getStock() + quantity);
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
}

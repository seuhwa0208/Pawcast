package com.project.pawcast.product.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {
    // Product에 들어갈 기능이지만 일단은 구현 여부를 몰라 구현해둠
    private final ProductVariantRepository productVariantRepository;

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
}
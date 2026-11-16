package com.project.powcast.product.dto;
import com.project.powcast.product.entity.InventoryTransaction;
import java.time.LocalDateTime;
public record StockAdjustmentResponse(Long id, Long variantId, Long memberId, int quantityChange, int stockBefore, int stockAfter, String reason, LocalDateTime createdAt) {
    public static StockAdjustmentResponse from(InventoryTransaction t) {
        return new StockAdjustmentResponse(t.getId(), t.getVariant().getId(), t.getMember()==null?null:t.getMember().getId(), t.getQuantityChange(), t.getStockBefore(), t.getStockAfter(), t.getReferenceType(), t.getCreatedAt());
    }
}

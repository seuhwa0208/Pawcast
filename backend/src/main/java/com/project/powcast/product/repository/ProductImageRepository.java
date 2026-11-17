package com.project.powcast.product.repository;
import com.project.powcast.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findAllByProductIdOrderBySortOrderAscIdAsc(Long productId);
    void deleteAllByProductId(Long productId);

    List<ProductImage> findAllByProductIdInOrderByMainDescSortOrderAscIdAsc(List<Long> productIds);
}

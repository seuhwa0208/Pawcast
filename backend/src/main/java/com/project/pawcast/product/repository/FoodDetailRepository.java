package com.project.pawcast.product.repository;
import com.project.pawcast.product.entity.FoodDetail;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FoodDetailRepository extends JpaRepository<FoodDetail, Long> {
    
}

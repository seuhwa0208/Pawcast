package com.project.powcast.product.repository;
import com.project.powcast.product.entity.FoodDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FoodDetailRepository extends JpaRepository<FoodDetail, Long> {
    
}

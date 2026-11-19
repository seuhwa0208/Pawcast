package com.project.pawcast.product.repository;
import com.project.pawcast.product.entity.DeviceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DeviceDetailRepository extends JpaRepository<DeviceDetail, Long> {
    
}

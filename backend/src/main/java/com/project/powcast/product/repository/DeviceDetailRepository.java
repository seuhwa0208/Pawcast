package com.project.powcast.product.repository;
import com.project.powcast.product.entity.DeviceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DeviceDetailRepository extends JpaRepository<DeviceDetail, Long> {
    
}

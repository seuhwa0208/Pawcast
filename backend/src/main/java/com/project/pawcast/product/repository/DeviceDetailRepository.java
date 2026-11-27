package com.project.pawcast.product.repository;

import com.project.pawcast.product.entity.DeviceDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceDetailRepository extends JpaRepository<DeviceDetail, Long> {
    
}

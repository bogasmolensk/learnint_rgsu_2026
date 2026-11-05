package com.company.equipment_request_service.repository;

import com.company.equipment_request_service.entity.EquipmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EquipmentRequestRepository
        extends JpaRepository<EquipmentRequest, Long>,
        JpaSpecificationExecutor<EquipmentRequest> {
}
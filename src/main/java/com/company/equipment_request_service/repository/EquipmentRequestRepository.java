package com.company.equipment_request_service.repository;

import com.company.equipment_request_service.entity.EquipmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRequestRepository extends JpaRepository<EquipmentRequest, Long> {
}
package com.company.equipment_request_service.repository;

import com.company.equipment_request_service.entity.RequestStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestStatusHistoryRepository extends JpaRepository<RequestStatusHistory, Long> {
}
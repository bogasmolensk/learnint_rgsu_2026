package com.company.equipment_request_service.entity;

public enum RequestStatus {
    DRAFT,          // Черновик
    UNDER_REVIEW,   // На рассмотрении
    APPROVED,       // Одобрено
    REJECTED,       // Отклонено
    CANCELLED       // Отменено
}
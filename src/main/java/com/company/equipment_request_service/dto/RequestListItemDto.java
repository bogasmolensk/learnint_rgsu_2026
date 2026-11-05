package com.company.equipment_request_service.dto;

import com.company.equipment_request_service.entity.EquipmentRequest;
import com.company.equipment_request_service.entity.RequestStatus;
import java.time.Instant;

public record RequestListItemDto(
        Long id,
        Long authorId,
        String equipmentName,
        Integer quantity,
        RequestStatus status,
        Instant createdAt,
        Integer version
) {
    public static RequestListItemDto from(EquipmentRequest r) {
        return new RequestListItemDto(
                r.getId(),
                r.getAuthor().getId(),
                r.getEquipmentName(),
                r.getQuantity(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getVersion()
        );
    }
}
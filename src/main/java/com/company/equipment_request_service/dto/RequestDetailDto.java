package com.company.equipment_request_service.dto;

import com.company.equipment_request_service.entity.EquipmentRequest;
import com.company.equipment_request_service.entity.RequestStatus;
import java.time.Instant;

public record RequestDetailDto(
        Long id,
        Long authorId,

        String equipmentName,
        Integer quantity,
        String justification,
        RequestStatus status,
        String decisionComment,
        Instant createdAt,
        Instant updatedAt,
        Integer version
) {

    public static RequestDetailDto from(EquipmentRequest r) {
        return new RequestDetailDto(
                r.getId(),
                r.getAuthor().getId(),

                r.getEquipmentName(),
                r.getQuantity(),
                r.getJustification(),
                r.getStatus(),
                r.getDecisionComment(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getVersion()
        );
    }
}
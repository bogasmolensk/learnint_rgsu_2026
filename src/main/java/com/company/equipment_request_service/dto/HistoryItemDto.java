package com.company.equipment_request_service.dto;

import com.company.equipment_request_service.entity.RequestStatus;
import com.company.equipment_request_service.entity.RequestStatusHistory;
import java.time.Instant;

public record HistoryItemDto(
        Long changedBy,
        RequestStatus previousStatus,
        RequestStatus newStatus,
        String comment,
        Instant createdAt
) {
    public static HistoryItemDto from(RequestStatusHistory h) {
        return new HistoryItemDto(
                h.getChangedBy().getId(),
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getComment(),
                h.getCreatedAt()
        );
    }
}
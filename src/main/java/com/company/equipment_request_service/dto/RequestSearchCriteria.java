package com.company.equipment_request_service.dto;

public record RequestSearchCriteria(
        String scope,
        String status,
        String from,
        String to,
        Integer page,
        Integer size,
        String sort
) {}
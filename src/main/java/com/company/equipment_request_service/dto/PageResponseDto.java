package com.company.equipment_request_service.dto;

import java.util.List;

public record PageResponseDto<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {
    public static <T> PageResponseDto<T> of(org.springframework.data.domain.Page<T> page) {
        return new PageResponseDto<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
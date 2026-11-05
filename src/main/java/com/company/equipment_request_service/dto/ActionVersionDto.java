package com.company.equipment_request_service.dto;

import jakarta.validation.constraints.NotNull;

public record ActionVersionDto(
        @NotNull(message = "Поле version обязательно для изменения")
        Integer version
) {}
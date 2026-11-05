package com.company.equipment_request_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateRequestDto(
        @NotNull(message = "Поле version обязательно для изменения")
        Integer version,

        @Size(max = 200, message = "Название не длиннее 200 символов")
        String equipmentName,

        @Positive(message = "Количество должно быть больше нуля")
        Integer quantity,

        String justification
) {}
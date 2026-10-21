package com.company.equipment_request_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRequestDto(

        @NotBlank(message = "Название оборудования обязательно")

        @Size(max = 200, message = "Название не длиннее 200 символов")
        String equipmentName,

        @Positive(message = "Количество должно быть больше нуля")
        Integer quantity,

        @NotBlank(message = "Обоснование обязательно")
        String justification
) {}
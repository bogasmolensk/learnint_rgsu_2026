package com.company.equipment_request_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RejectRequestDto(
        @NotBlank(message = "Комментарий при отклонении обязателен")
        @Size(max = 500, message = "Комментарий не длиннее 500 символов")
        String comment,

        @NotNull(message = "Поле version обязательно для изменения")
        Integer version
) {}
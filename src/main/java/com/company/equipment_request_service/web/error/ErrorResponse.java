package com.company.equipment_request_service.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String error,
        String message,
        List<FieldErrorDto> fieldErrors
) {
    public ErrorResponse(String error, String message) {
        this(error, message, List.of());
    }
}
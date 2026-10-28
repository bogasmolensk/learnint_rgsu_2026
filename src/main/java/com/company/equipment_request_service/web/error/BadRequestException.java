package com.company.equipment_request_service.web.error;

import java.util.List;

public class BadRequestException extends RuntimeException {
    private final String error;
    private final List<FieldErrorDto> fieldErrors;
    public BadRequestException(String error, String message, List<FieldErrorDto> fieldErrors) {
        super(message);
        this.error = error;
        this.fieldErrors = fieldErrors;
    }
    public String getError() { return error; }
    public List<FieldErrorDto> getFieldErrors() { return fieldErrors; }
}


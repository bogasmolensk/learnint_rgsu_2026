package com.company.equipment_request_service.web.error;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}

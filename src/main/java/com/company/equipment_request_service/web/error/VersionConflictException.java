package com.company.equipment_request_service.web.error;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException(String message) { super(message); }
}


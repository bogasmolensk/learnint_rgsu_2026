package com.company.equipment_request_service.web.error;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(Long id) {
        super("Заявка с id=" + id + " не найдена");
    }
}
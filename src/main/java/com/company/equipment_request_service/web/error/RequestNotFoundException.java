package com.company.equipment_request_service.web.error;

import java.util.List;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(Long id) {
        super("Заявка с id=" + id + " не найдена");
    }
}


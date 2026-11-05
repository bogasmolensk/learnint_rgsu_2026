package com.company.equipment_request_service.web.error;

public class CurrentUserNotFoundException extends RuntimeException {
    public CurrentUserNotFoundException(Long id) {
        super("Текущий пользователь id=" + id + " не найден");
    }
}
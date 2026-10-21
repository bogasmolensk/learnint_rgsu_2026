package com.company.equipment_request_service.security;

import com.company.equipment_request_service.entity.UserRole;
import org.springframework.stereotype.Component;

@Component

public class TestCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser currentUser() {
        return new CurrentUser(1L, UserRole.EMPLOYEE);
    }
}
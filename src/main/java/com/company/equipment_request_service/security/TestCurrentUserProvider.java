package com.company.equipment_request_service.security;

import com.company.equipment_request_service.entity.UserRole;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("stub-user")   // активна только когда явно включён stub-user (интеграционные тесты недели 6)
public class TestCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser currentUser() {
        return new CurrentUser(1L, UserRole.EMPLOYEE);
    }
}
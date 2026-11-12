package com.company.equipment_request_service.security;

import com.company.equipment_request_service.entity.UserRole;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Profile("!stub-user")   // активен, когда профиль stub-user НЕ включён (прод и тесты безопасности)
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUserDetails u)) {
            throw new IllegalStateException("Нет аутентификации в контексте — защищённый путь вызван без входа");
        }
        return new CurrentUser(u.getId(), u.getRole());
    }
}
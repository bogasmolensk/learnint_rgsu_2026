package com.company.equipment_request_service.security;

import com.company.equipment_request_service.entity.UserRole;

public record CurrentUser(Long id, UserRole role) {
}

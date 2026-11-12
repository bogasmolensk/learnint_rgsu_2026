package com.company.equipment_request_service.security;

import com.company.equipment_request_service.entity.User;
import com.company.equipment_request_service.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String login;
    private final String passwordHash;
    private final UserRole role;

    public AppUserDetails(User u) {
        this.id = u.getId();
        this.login = u.getLogin();
        this.passwordHash = u.getPassword();
        this.role = u.getRole();
    }

    public Long getId() { return id; }
    public UserRole getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security требует префикс ROLE_ для hasRole(...)
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return login; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
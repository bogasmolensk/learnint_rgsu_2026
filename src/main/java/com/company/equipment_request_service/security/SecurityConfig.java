package com.company.equipment_request_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();   // bcrypt-реализация PasswordEncoder
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                 // для stateless API + Basic CSRF не нужен (нет cookie-сессии)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ping").permitAll()     // технический эндпоинт открыт: curl /ping без пароля
                        .anyRequest().authenticated()             // всё остальное требует входа -> иначе 401 от фильтра
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, authEx) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"UNAUTHENTICATED\",\"message\":\"Требуется аутентификация\"}");
                }))
                .httpBasic(Customizer.withDefaults());        // схема Basic: заголовок Authorization: Base64(login:pass)
        return http.build();
    }
}
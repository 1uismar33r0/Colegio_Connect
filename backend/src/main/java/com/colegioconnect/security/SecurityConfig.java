package com.colegioconnect.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Open: registration, verification, login and the CSRF token
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/register", "/api/auth/verify",
                        "/api/auth/resend", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                // Open: health check, API docs and error pages
                .requestMatchers("/actuator/health", "/v3/api-docs/**",
                        "/swagger-ui.html", "/swagger-ui/**", "/error").permitAll()
                // Everything else needs a logged-in user
                .anyRequest().authenticated())
            .csrf(csrf -> csrf.spa())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));

        return http.build();
    }
}
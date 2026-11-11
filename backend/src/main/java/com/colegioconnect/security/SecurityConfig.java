package com.colegioconnect.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthorizationPolicy authz) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Open: registration, verification, login and the CSRF token
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/register", "/api/auth/verify",
                        "/api/auth/resend", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                // Open: health check, API docs and error pages
                .requestMatchers("/actuator/health", "/actuator/health/**", "/v3/api-docs/**",
                        "/swagger-ui.html", "/swagger-ui/**", "/error").permitAll()
                // Only the React shell and its static assets are public browser GETs.
                // No /api, /actuator, or admin API path can match these patterns.
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/login", "/register",
                        "/verify", "/dashboard", "/forbidden", "/student", "/student/**",
                        "/institution", "/institution/**", "/company", "/company/**",
                        "/moderation", "/moderation/**", "/assets/**", "/favicon.ico").permitAll()
                // Everything else needs a logged-in user whose account is still active
                .anyRequest().access((authentication, context) -> {
                    Authentication user = authentication.get();
                    boolean loggedIn = user != null
                            && user.isAuthenticated()
                            && !(user instanceof AnonymousAuthenticationToken);
                    return new AuthorizationDecision(loggedIn && authz.isActive(user));
                }))
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
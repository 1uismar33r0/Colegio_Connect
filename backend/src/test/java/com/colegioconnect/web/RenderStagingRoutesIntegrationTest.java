package com.colegioconnect.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.repository.UserRepository;

@SpringBootTest(properties = {"app.mail.provider=fake",
        "management.endpoint.health.probes.enabled=true",
        "management.endpoint.health.group.readiness.include=readinessState,db"})
@AutoConfigureMockMvc
@ActiveProfiles("render-staging")
@Import(TestcontainersConfiguration.class)
class RenderStagingRoutesIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;

    @Test
    void anonymousSpaDeepLinksForwardWhileApiAndHealthKeepTheirMeaning() throws Exception {
        for (String path : new String[]{"/", "/login", "/register", "/dashboard",
                "/student/discover", "/institution/overview", "/company/dashboard",
                "/moderation/review-queue"}) {
            mockMvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(forwardedUrl("/index.html"));
        }
        mockMvc.perform(get("/api/me/activity")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/not-a-route")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/assets/missing.js")).andExpect(status().isNotFound());
    }

    @Test
    void publicStagingRegistrationAndResendAreUnavailableWithoutCreatingAccounts() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"render.disabled@upr.edu\",\"password\":\"A synthetic staging password 123!\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
        mockMvc.perform(post("/api/auth/resend").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"render.disabled@upr.edu\"}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(users.existsByEmail("render.disabled@upr.edu")).isFalse();
    }
}

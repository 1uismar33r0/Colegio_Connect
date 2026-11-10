package com.colegioconnect.listings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import com.colegioconnect.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DashboardOpenApiIntegrationTest {

    @Autowired private org.springframework.test.web.servlet.MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void fiveRoutesAndProposedDtoFieldsAppearInGeneratedContract() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode api = mapper.readTree(body);
        JsonNode paths = api.path("paths");
        for (String route : new String[] {
                "/api/listings/feed", "/api/me/activity", "/api/institution/overview",
                "/api/company/dashboard", "/api/admin/review-queue"
        }) {
            assertThat(paths.path(route).has("get")).as(route).isTrue();
        }
        JsonNode schemas = api.path("components").path("schemas");
        assertProperties(schemas, "FeedItem", "id", "title", "source", "postedAt",
                "type", "summary", "tags");
        assertProperties(schemas, "StudentActivityResponse", "activeResponses", "saved");
        assertProperties(schemas, "OfficeOverviewResponse", "stats", "recent");
        assertProperties(schemas, "CompanyDashboardResponse", "stats", "active", "attention");
        assertProperties(schemas, "ReviewQueueResponse", "stats", "items");
        assertProperties(schemas, "ReviewQueueStats", "awaitingReview", "reportedListings",
                "pendingVerification");
        assertThat(paths.path("/api/institution/overview").path("get").path("parameters")
                .toString()).contains("organizationId");
        assertThat(paths.path("/api/company/dashboard").path("get").path("parameters")
                .toString()).contains("organizationId");
    }

    private void assertProperties(JsonNode schemas, String name, String... properties) {
        JsonNode schema = schemas.path(name).path("properties");
        for (String property : properties) {
            assertThat(schema.has(property)).as(name + "." + property).isTrue();
        }
    }
}

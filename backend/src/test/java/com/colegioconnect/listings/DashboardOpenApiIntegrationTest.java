package com.colegioconnect.listings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import java.util.stream.StreamSupport;
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
        assertProperties(schemas, "FeedItem", "id", "title", "source", "sourceLabel", "postedAt",
                "type", "summary", "tags");
        assertProperties(schemas, "StudentActivityResponse", "activeResponses", "saved");
        assertProperties(schemas, "OfficeOverviewResponse", "stats", "recent");
        assertProperties(schemas, "CompanyDashboardResponse", "stats", "active", "attention");
        assertProperties(schemas, "ReviewQueueResponse", "stats", "items");
        assertProperties(schemas, "ReviewQueueStats", "awaitingReview", "reportedListings",
                "pendingVerification");
        assertThat(schemas.path("FeedItem").path("required").toString())
                .contains("id", "title", "source", "sourceLabel", "postedAt", "type", "summary", "tags");
        assertThat(schemas.path("ReviewQueueStats").path("required").toString())
                .contains("awaitingReview", "reportedListings", "pendingVerification");
        assertThat(schemas.path("OfficeRecentItem").path("properties")
                .path("newResponses").toString()).contains("null");
        assertThat(schemas.path("ReviewQueueItem").path("properties")
                .path("receivedAt").toString()).contains("null");
        JsonNode feedParameters = paths.path("/api/listings/feed").path("get").path("parameters");
        String feedText = feedParameters.toString();
        assertThat(feedText).contains("category", "keyword", "sourceLabel", "organizationId",
                "deadlineOnOrBefore", "type").doesNotContain("\"name\":\"field\"");
        boolean deprecatedAlias = StreamSupport.stream(feedParameters.spliterator(), false)
                .anyMatch(parameter -> "type".equals(parameter.path("name").asText())
                        && parameter.path("deprecated").asBoolean());
        assertThat(deprecatedAlias).isTrue();
        assertProperties(schemas, "CompanyStats", "activeListings", "responses", "approvalStatus");
        assertThat(schemas.path("CompanyStats").path("properties").has("profileStatus")).isFalse();
        assertThat(schemas.path("ReviewReason").toString()).doesNotContain("PROFILE_VERIFICATION");
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

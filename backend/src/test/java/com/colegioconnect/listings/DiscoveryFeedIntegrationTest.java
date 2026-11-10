package com.colegioconnect.listings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DiscoveryFeedIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void deleteListings() {
        jdbc.update("DELETE FROM listings");
    }

    @Test
    void authenticatedUserGetsEmptyPersistedFeed() throws Exception {
        var student = activeVerifiedUser("feed.student@upr.edu", Role.STUDENT);
        mockMvc.perform(get("/api/listings/feed").session(login(student, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void onlyCurrentPublishedEligibleSourcesAppearWithoutPrivateFields() throws Exception {
        User student = activeVerifiedUser("feed.reader@upr.edu", Role.STUDENT);
        Organization office = organization("Research Office", OrgType.UPRM_OFFICE,
                ApprovalStatus.APPROVED);
        User publisher = activeVerifiedUser("feed.publisher@upr.edu");
        assign(publisher, Role.INSTITUTIONAL, office);
        UUID visible = listing(publisher, office, "Research position", "AI lab project",
                "RESEARCH_POSITION", "Computing", "PUBLISHED", 1);
        jdbc.update("UPDATE listings SET tags = ARRAY['research', 'ai'] WHERE id = ?", visible);
        listing(publisher, office, "Hidden job", "secret", "JOB", "Computing", "HIDDEN", 1);
        UUID restricted = listing(publisher, office, "Restricted job", "secret", "JOB",
                "Computing", "PUBLISHED", 1);
        jdbc.update("UPDATE listings SET visibility = 'RESTRICTED' WHERE id = ?", restricted);
        listing(publisher, office, "Draft job", "secret", "JOB", "Computing", "DRAFT", 1);
        listing(publisher, office, "Expired job", "secret", "JOB", "Computing", "PUBLISHED", -1);
        Organization pending = organization("Unapproved company", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.PENDING);
        User company = new User("pending.publisher@example.com",
                passwordEncoder.encode(TEST_PASSWORD), AccountType.EXTERNAL_ORGANIZATION);
        company.setEmailVerified(true);
        company = userRepository.save(company);
        assign(company, Role.COMPANY, pending);
        listing(company, pending, "Pending company job", "secret", "JOB", "Computing",
                "PUBLISHED", 1);

        String response = mockMvc.perform(get("/api/listings/feed")
                        .session(login(student, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visible.toString()))
                .andExpect(jsonPath("$[0].source").value("Research Office"))
                .andExpect(jsonPath("$[0].type").value("Research position"))
                .andExpect(jsonPath("$[0].tags.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).contains("postedAt").doesNotContain(
                "passwordHash", "publisherUserId", "pending.publisher@example.com",
                "feed.publisher@upr.edu");
    }

    @Test
    void filtersAndStablePaginationUsePersistedRows() throws Exception {
        User student = activeVerifiedUser("filters.reader@upr.edu", Role.STUDENT);
        Organization office = organization("Office", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);
        User publisher = activeVerifiedUser("filters.publisher@upr.edu");
        assign(publisher, Role.INSTITUTIONAL, office);
        Instant posted = Instant.now().minus(1, ChronoUnit.HOURS);
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        listing(first, publisher, office, "AI research", "Data science role", "JOB",
                "Computing", "PUBLISHED", posted, posted.plus(7, ChronoUnit.DAYS));
        listing(second, publisher, office, "AI engineering", "Machine learning role", "JOB",
                "Computing", "PUBLISHED", posted, posted.plus(7, ChronoUnit.DAYS));
        listing(publisher, office, "Biology study", "Lab participation",
                "RESEARCH_PARTICIPATION", "Biology", "PUBLISHED", 7);
        var session = login(student, status().isOk());

        mockMvc.perform(get("/api/listings/feed?keyword=AI&type=JOB&field=COMPUTING&page=0&size=1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(second.toString()));
        mockMvc.perform(get("/api/listings/feed?keyword=AI&type=JOB&field=COMPUTING&page=1&size=1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(first.toString()));
        mockMvc.perform(get("/api/listings/feed?type=RESEARCH_PARTICIPATION").session(session))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/listings/feed").param("type", "Research participation")
                        .session(session))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/listings/feed?type=Event").session(session))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/listings/feed?size=101").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousAndSuspendedUsersCannotReadFeed() throws Exception {
        User suspended = activeVerifiedUser("feed.suspended@upr.edu", Role.STUDENT);
        var session = login(suspended, status().isOk());
        suspended.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(suspended);
        mockMvc.perform(get("/api/listings/feed"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/listings/feed").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void allSixApprovedOpportunityTypesUseTheSameFeed() throws Exception {
        User student = activeVerifiedUser("six.reader@upr.edu", Role.STUDENT);
        Organization office = organization("Six types office", OrgType.UPRM_OFFICE,
                ApprovalStatus.APPROVED);
        User publisher = activeVerifiedUser("six.publisher@upr.edu");
        assign(publisher, Role.INSTITUTIONAL, office);
        for (OpportunityType type : OpportunityType.values()) {
            listing(publisher, office, type.name() + " opportunity", "Synthetic summary",
                    type.name(), "Computing", "PUBLISHED", 7);
        }

        mockMvc.perform(get("/api/listings/feed?size=10")
                        .session(login(student, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[*].type", org.hamcrest.Matchers.containsInAnyOrder(
                        "Job", "Internship", "Co-op", "Research position",
                        "Research participation", "Capstone sponsorship")));
    }

    private UUID listing(User publisher, Organization organization, String title, String summary,
                         String type, String field, String status, int closingDays) {
        UUID id = UUID.randomUUID();
        Instant posted = Instant.now().minus(1, ChronoUnit.HOURS);
        listing(id, publisher, organization, title, summary, type, field, status,
                posted, Instant.now().plus(closingDays, ChronoUnit.DAYS));
        return id;
    }

    private void listing(UUID id, User publisher, Organization organization, String title,
                         String summary, String type, String field, String status,
                         Instant posted, Instant closes) {
        jdbc.update("INSERT INTO listings (id, publisher_user_id, organization_id, title, summary, "
                        + "opportunity_type, field, status, visibility, posted_at, closes_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PUBLIC', ?, ?)",
                id, publisher.getId(), organization.getId(), title, summary, type, field,
                status, "DRAFT".equals(status) ? null : Timestamp.from(posted),
                Timestamp.from(closes));
    }
}

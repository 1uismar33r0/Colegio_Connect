package com.colegioconnect.listings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OfficeOverviewIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void deleteOverviewRecords() {
        jdbc.update("DELETE FROM listing_responses");
        jdbc.update("DELETE FROM saved_listings");
        jdbc.update("DELETE FROM listings");
    }

    @Test
    void authorizedOfficeGetsEmptyPersistedOverview() throws Exception {
        var office = organization("Office overview", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);
        var member = activeVerifiedUser("office.overview@upr.edu");
        assign(member, Role.INSTITUTIONAL, office);
        mockMvc.perform(get("/api/institution/overview?organizationId=" + office.getId())
                        .session(login(member, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.published").value(0))
                .andExpect(jsonPath("$.stats.newResponses").value(0))
                .andExpect(jsonPath("$.stats.drafts").value(0))
                .andExpect(jsonPath("$.recent.length()").value(0));
        mockMvc.perform(get("/api/institution/overview")
                        .session(login(member, status().isOk())))
                .andExpect(status().isOk());
    }

    @Test
    void selectedOrganizationHasAccurateCountsAndDraftNullability() throws Exception {
        Organization first = organization("First office", OrgType.UPRM_OFFICE,
                ApprovalStatus.APPROVED);
        Organization second = organization("Second office", OrgType.UPRM_DEPARTMENT,
                ApprovalStatus.APPROVED);
        User member = activeVerifiedUser("office.member@upr.edu");
        assign(member, Role.INSTITUTIONAL, first);
        assign(member, Role.INSTITUTIONAL, second);
        UUID published = listing(member, first, "Published project", "PUBLISHED");
        UUID draft = listing(member, first, "Draft project", "DRAFT");
        listing(member, first, "Closed project", "CLOSED");
        UUID expired = listing(member, first, "Expired project", "PUBLISHED");
        jdbc.update("UPDATE listings SET closes_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)), expired);
        listing(member, second, "Other office project", "PUBLISHED");
        User student = activeVerifiedUser("office.respondent@upr.edu", Role.STUDENT);
        jdbc.update("INSERT INTO listing_responses (listing_id, user_id, status) "
                        + "VALUES (?, ?, 'ACTIVE')", published, student.getId());
        var session = login(member, status().isOk());

        mockMvc.perform(get("/api/institution/overview").session(session))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/institution/overview?organizationId=" + first.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.published").value(1))
                .andExpect(jsonPath("$.stats.drafts").value(1))
                .andExpect(jsonPath("$.stats.newResponses").value(1))
                .andExpect(jsonPath("$.recent.length()").value(4))
                .andExpect(jsonPath("$.recent[?(@.id=='" + expired + "')].status")
                        .value(org.hamcrest.Matchers.contains("CLOSED")))
                .andExpect(jsonPath("$.recent[?(@.id=='" + draft + "')].newResponses")
                        .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
        mockMvc.perform(get("/api/institution/overview?organizationId=" + second.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.published").value(1))
                .andExpect(jsonPath("$.stats.newResponses").value(0))
                .andExpect(jsonPath("$.recent.length()").value(1));

        roleAssignmentRepository.deleteAll(roleAssignmentRepository.findByUserId(member.getId())
                .stream().filter(role -> role.getOrganization() != null
                        && role.getOrganization().getId().equals(second.getId())).toList());
        mockMvc.perform(get("/api/institution/overview?organizationId=" + second.getId())
                        .session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/institution/overview?organizationId=" + first.getId())
                        .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void unaffiliatedAndSuspendedMembersAreDenied() throws Exception {
        Organization office = organization("Office", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);
        User member = activeVerifiedUser("office.suspended@upr.edu");
        assign(member, Role.INSTITUTIONAL, office);
        var session = login(member, status().isOk());
        mockMvc.perform(get("/api/institution/overview?organizationId=" + office.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/institution/overview?organizationId=" + UUID.randomUUID())
                        .session(session))
                .andExpect(status().isForbidden());
        member.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(member);
        mockMvc.perform(get("/api/institution/overview?organizationId=" + office.getId())
                        .session(session))
                .andExpect(status().isForbidden());
    }

    private UUID listing(User publisher, Organization organization, String title, String status) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("INSERT INTO listings (id, publisher_user_id, organization_id, title, "
                        + "summary, opportunity_type, field, status, posted_at, closes_at) "
                        + "VALUES (?, ?, ?, ?, 'Synthetic summary', 'JOB', 'Computing', ?, ?, ?)",
                id, publisher.getId(), organization.getId(), title, status,
                "DRAFT".equals(status) ? null : Timestamp.from(now.minusSeconds(60)),
                Timestamp.from(now.plusSeconds(86400)));
        return id;
    }
}

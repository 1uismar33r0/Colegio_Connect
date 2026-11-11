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
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReviewQueueIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void deleteReviewRecords() {
        jdbc.update("DELETE FROM listing_reports");
        jdbc.update("DELETE FROM listing_responses");
        jdbc.update("DELETE FROM saved_listings");
        jdbc.update("DELETE FROM listings");
    }

    @Test
    void currentModeratorGetsEmptyPersistedReviewQueue() throws Exception {
        var moderator = activeVerifiedUser("review.queue@upr.edu", Role.MODERATOR);
        mockMvc.perform(get("/api/admin/review-queue")
                        .session(login(moderator, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.awaitingReview").value(0))
                .andExpect(jsonPath("$.stats.reportedListings").value(0))
                .andExpect(jsonPath("$.stats.pendingVerification").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void actualPendingCompanyAndReportedListingAppearWithoutReporterDetails() throws Exception {
        User moderator = activeVerifiedUser("queue.moderator@upr.edu", Role.MODERATOR);
        Organization pending = organization("Pending company", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.PENDING);
        organization("Approved company", OrgType.EXTERNAL_COMPANY, ApprovalStatus.APPROVED);
        Organization office = organization("Research office", OrgType.UPRM_OFFICE,
                ApprovalStatus.APPROVED);
        User publisher = activeVerifiedUser("queue.publisher@upr.edu");
        assign(publisher, Role.INSTITUTIONAL, office);
        UUID listing = UUID.randomUUID();
        jdbc.update("INSERT INTO listings (id, publisher_user_id, organization_id, title, "
                        + "summary, opportunity_type, trust_label, status) "
                        + "VALUES (?, ?, ?, 'Reported opportunity', 'Synthetic summary', "
                        + "'JOB', 'VERIFIED_UPRM_INSTITUTION', 'DRAFT')",
                listing, publisher.getId(), office.getId());
        User reporter = activeVerifiedUser("private.reporter@upr.edu", Role.STUDENT);
        User secondReporter = activeVerifiedUser("private.second@upr.edu", Role.STUDENT);
        jdbc.update("INSERT INTO listing_reports (listing_id, reporter_user_id, detail, status) "
                        + "VALUES (?, ?, 'Synthetic report', 'OPEN'), "
                        + "(?, ?, 'Other synthetic report', 'OPEN')",
                listing, reporter.getId(), listing, secondReporter.getId());
        Instant old = Instant.now().minusSeconds(3600);
        jdbc.update("UPDATE organizations SET review_received_at = ? WHERE id = ?",
                Timestamp.from(old), pending.getId());
        var session = login(moderator, status().isOk());

        String response = mockMvc.perform(get("/api/admin/review-queue?page=0&size=1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.awaitingReview").value(2))
                .andExpect(jsonPath("$.stats.reportedListings").value(1))
                .andExpect(jsonPath("$.stats.pendingVerification").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].reason").value("REPORTED_LISTING"))
                .andExpect(jsonPath("$.items[0].submittedBy").value("Research office"))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(response)
                .contains("receivedAt").doesNotContain(reporter.getEmail(),
                        secondReporter.getEmail(), "Synthetic report");
        mockMvc.perform(get("/api/admin/review-queue?page=1&size=1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(pending.getId().toString()))
                .andExpect(jsonPath("$.items[0].reason").value("NEW_ORGANIZATION"));
        mockMvc.perform(get("/api/admin/review-queue?size=101").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousStudentSuspendedAndRevokedModeratorAreDenied() throws Exception {
        User moderator = activeVerifiedUser("queue.revoked@upr.edu", Role.MODERATOR);
        User student = activeVerifiedUser("queue.student@upr.edu", Role.STUDENT);
        var session = login(moderator, status().isOk());
        mockMvc.perform(get("/api/admin/review-queue"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/review-queue")
                        .session(login(student, status().isOk())))
                .andExpect(status().isForbidden());
        roleAssignmentRepository.deleteAll(roleAssignmentRepository.findByUserId(moderator.getId()));
        mockMvc.perform(get("/api/admin/review-queue").session(session))
                .andExpect(status().isForbidden());
        assign(moderator, Role.MODERATOR, null);
        moderator.setStatus(UserStatus.ACTIVE);
        moderator.setEmailVerified(false);
        userRepository.saveAndFlush(moderator);
        mockMvc.perform(get("/api/admin/review-queue").session(session))
                .andExpect(status().isForbidden());

        User external = new User("queue.external@example.com",
                passwordEncoder.encode(TEST_PASSWORD), AccountType.EXTERNAL_ORGANIZATION);
        external.setEmailVerified(true);
        external = userRepository.save(external);
        assign(external, Role.MODERATOR, null); // Existing malformed row must not grant authority.
        mockMvc.perform(get("/api/admin/review-queue")
                        .session(login(external, status().isOk())))
                .andExpect(status().isForbidden());
        moderator.setEmailVerified(true);
        moderator.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(moderator);
        mockMvc.perform(get("/api/admin/review-queue").session(session))
                .andExpect(status().isForbidden());
    }
}

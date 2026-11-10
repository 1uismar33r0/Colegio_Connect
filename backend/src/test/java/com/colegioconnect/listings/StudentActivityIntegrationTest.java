package com.colegioconnect.listings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class StudentActivityIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void deleteActivityRecords() {
        jdbc.update("DELETE FROM listing_responses");
        jdbc.update("DELETE FROM saved_listings");
        jdbc.update("DELETE FROM listings");
    }

    @Test
    void verifiedStudentGetsRealEmptyActivityCounts() throws Exception {
        var student = activeVerifiedUser("activity.student@upr.edu", Role.STUDENT);
        mockMvc.perform(get("/api/me/activity").session(login(student, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeResponses").value(0))
                .andExpect(jsonPath("$.saved").value(0));
    }

    @Test
    void countsOnlyCurrentStudentsPersistedSavesAndActiveResponses() throws Exception {
        User first = activeVerifiedUser("activity.first@upr.edu", Role.STUDENT);
        User second = activeVerifiedUser("activity.second@upr.edu", Role.STUDENT);
        Organization office = organization("Activity office", OrgType.UPRM_OFFICE,
                ApprovalStatus.APPROVED);
        User publisher = activeVerifiedUser("activity.publisher@upr.edu");
        assign(publisher, Role.INSTITUTIONAL, office);
        UUID one = listing(publisher, office);
        UUID two = listing(publisher, office);
        jdbc.update("INSERT INTO saved_listings (user_id, listing_id) VALUES (?, ?), (?, ?), (?, ?)",
                first.getId(), one, first.getId(), two, second.getId(), one);
        jdbc.update("INSERT INTO listing_responses (listing_id, user_id, status) "
                        + "VALUES (?, ?, 'ACTIVE'), (?, ?, 'WITHDRAWN'), (?, ?, 'ACTIVE')",
                one, first.getId(), two, first.getId(), two, second.getId());

        mockMvc.perform(get("/api/me/activity?userId=" + second.getId())
                        .session(login(first, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeResponses").value(1))
                .andExpect(jsonPath("$.saved").value(2));
        mockMvc.perform(get("/api/me/activity").session(login(second, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeResponses").value(1))
                .andExpect(jsonPath("$.saved").value(1));
    }

    @Test
    void anonymousWrongRoleSuspendedAndRevokedStudentAreDenied() throws Exception {
        User student = activeVerifiedUser("activity.revoked@upr.edu", Role.STUDENT);
        User institutional = activeVerifiedUser("activity.institutional@upr.edu");
        Organization office = organization("Activity office", OrgType.UPRM_OFFICE,
                ApprovalStatus.APPROVED);
        assign(institutional, Role.INSTITUTIONAL, office);
        var studentSession = login(student, status().isOk());

        mockMvc.perform(get("/api/me/activity")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/activity")
                        .session(login(institutional, status().isOk())))
                .andExpect(status().isForbidden());
        roleAssignmentRepository.deleteAll(roleAssignmentRepository.findByUserId(student.getId()));
        mockMvc.perform(get("/api/me/activity").session(studentSession))
                .andExpect(status().isForbidden());
        assign(student, Role.STUDENT, null);
        student.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(student);
        mockMvc.perform(get("/api/me/activity").session(studentSession))
                .andExpect(status().isForbidden());
    }

    private UUID listing(User publisher, Organization organization) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO listings (id, publisher_user_id, organization_id, title, "
                        + "summary, opportunity_type, field, status) "
                        + "VALUES (?, ?, ?, 'Synthetic activity listing', 'Synthetic summary', "
                        + "'JOB', 'Computing', 'DRAFT')",
                id, publisher.getId(), organization.getId());
        return id;
    }
}

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
import com.colegioconnect.accounts.entity.AccountType;
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
class CompanyDashboardIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void deleteDashboardRecords() {
        jdbc.update("DELETE FROM company_application_owners");
        jdbc.update("DELETE FROM listing_responses");
        jdbc.update("DELETE FROM saved_listings");
        jdbc.update("DELETE FROM listings");
    }

    @Test
    void authorizedCompanyGetsEmptyPersistedDashboard() throws Exception {
        var company = organization("Approved company", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.APPROVED);
        var member = new User("company.dashboard@example.com",
                passwordEncoder.encode(TEST_PASSWORD), AccountType.EXTERNAL_ORGANIZATION);
        member.setEmailVerified(true);
        member = userRepository.save(member);
        assign(member, Role.COMPANY, company);
        mockMvc.perform(get("/api/company/dashboard?organizationId=" + company.getId())
                        .session(login(member, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.activeListings").value(0))
                .andExpect(jsonPath("$.stats.responses").value(0))
                .andExpect(jsonPath("$.stats.approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.active.length()").value(0))
                .andExpect(jsonPath("$.attention.newResponses").value(0));
        mockMvc.perform(get("/api/company/dashboard")
                        .session(login(member, status().isOk())))
                .andExpect(status().isOk());
    }

    @Test
    void approvedCompanySeesOnlyItsOwnActiveListingsAndResponseCounts() throws Exception {
        Organization company = organization("Company one", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.APPROVED);
        Organization other = organization("Company two", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.APPROVED);
        User member = companyMember("company.one@example.com", company);
        User otherMember = companyMember("company.two@example.com", other);
        UUID active = listing(member, company, "Active job", "PUBLISHED", 7);
        listing(member, company, "Expired job", "PUBLISHED", -1);
        listing(member, company, "Draft job", "DRAFT", 7);
        UUID otherListing = listing(otherMember, other, "Other company job", "PUBLISHED", 7);
        User student = activeVerifiedUser("company.respondent@upr.edu", Role.STUDENT);
        jdbc.update("INSERT INTO listing_responses (listing_id, user_id, status) "
                        + "VALUES (?, ?, 'ACTIVE'), (?, ?, 'WITHDRAWN')",
                active, student.getId(), otherListing, student.getId());
        String response = mockMvc.perform(get("/api/company/dashboard?organizationId="
                        + company.getId()).session(login(member, status().isOk())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.activeListings").value(1))
                .andExpect(jsonPath("$.stats.responses").value(1))
                .andExpect(jsonPath("$.active.length()").value(1))
                .andExpect(jsonPath("$.active[0].id").value(active.toString()))
                .andExpect(jsonPath("$.active[0].responses").value(1))
                .andExpect(jsonPath("$.attention.newResponses").value(1))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(response)
                .contains("closesAt").doesNotContain(student.getEmail(), "sharedFields");
    }

    @Test
    void pendingWrongOrganizationRevokedAndSuspendedCompanyAreDenied() throws Exception {
        Organization approved = organization("Approved", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.APPROVED);
        Organization pending = organization("Pending", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.PENDING);
        User member = companyMember("company.guard@example.com", approved);
        // The fixture bypasses the assignment API to verify defense in depth.
        assign(member, Role.COMPANY, pending);
        var session = login(member, status().isOk());
        mockMvc.perform(get("/api/company/dashboard?organizationId=" + approved.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/company/dashboard?organizationId=" + pending.getId())
                        .session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/company/dashboard?organizationId=" + UUID.randomUUID())
                        .session(session))
                .andExpect(status().isForbidden());
        roleAssignmentRepository.deleteAll(roleAssignmentRepository.findByUserId(member.getId())
                .stream().filter(role -> role.getOrganization() != null
                        && role.getOrganization().getId().equals(approved.getId())).toList());
        mockMvc.perform(get("/api/company/dashboard?organizationId=" + approved.getId())
                        .session(session))
                .andExpect(status().isForbidden());
        assign(member, Role.COMPANY, approved);
        member.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(member);
        mockMvc.perform(get("/api/company/dashboard?organizationId=" + approved.getId())
                        .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingOwnerGetsReadOnlyStatusWithoutCompanyRoleOrOtherCompanyData() throws Exception {
        Organization pending = organization("Applicant", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.PENDING);
        Organization other = organization("Other applicant", OrgType.EXTERNAL_COMPANY,
                ApprovalStatus.PENDING);
        User owner = new User("applicant@example.com", passwordEncoder.encode(TEST_PASSWORD),
                AccountType.EXTERNAL_ORGANIZATION);
        owner.setEmailVerified(true);
        owner = userRepository.save(owner);
        jdbc.update("INSERT INTO company_application_owners (organization_id, user_id) "
                + "VALUES (?, ?)", pending.getId(), owner.getId());
        var session = login(owner, status().isOk());
        mockMvc.perform(get("/api/company/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.stats.activeListings").value(0))
                .andExpect(jsonPath("$.active.length()").value(0));
        mockMvc.perform(get("/api/company/dashboard?organizationId=" + other.getId())
                .session(session)).andExpect(status().isForbidden());
        owner.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(owner);
        mockMvc.perform(get("/api/company/dashboard").session(session))
                .andExpect(status().isForbidden());
    }

    private User companyMember(String email, Organization organization) {
        User member = new User(email, passwordEncoder.encode(TEST_PASSWORD),
                AccountType.EXTERNAL_ORGANIZATION);
        member.setEmailVerified(true);
        member = userRepository.save(member);
        assign(member, Role.COMPANY, organization);
        return member;
    }

    private UUID listing(User publisher, Organization organization, String title,
                         String status, int closingDays) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("INSERT INTO listings (id, publisher_user_id, organization_id, title, "
                        + "summary, opportunity_type, trust_label, status, posted_at, closes_at) "
                        + "VALUES (?, ?, ?, ?, 'Synthetic summary', 'JOB', 'VERIFIED_UPRM_INSTITUTION', ?, ?, ?)",
                id, publisher.getId(), organization.getId(), title, status,
                "DRAFT".equals(status) ? null : Timestamp.from(now.minusSeconds(60)),
                Timestamp.from(now.plusSeconds(86400L * closingDays)));
        return id;
    }
}

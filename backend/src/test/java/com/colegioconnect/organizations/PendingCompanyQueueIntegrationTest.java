package com.colegioconnect.organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditEventRepository;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PendingCompanyQueueIntegrationTest {
    private static final String PASSWORD = "a long queue test password";

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizations;
    @Autowired private AuditEventRepository auditEvents;
    @Autowired private UserRepository users;
    @Autowired private RoleAssignmentRepository roles;
    @Autowired private EmailCodeRepository codes;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        auditEvents.deleteAll();
        roles.deleteAll();
        organizations.deleteAll();
        codes.deleteAll();
        users.deleteAll();
    }

    @Test
    void moderatorSeesOnlyPendingExternalCompaniesWithMinimalFields() throws Exception {
        User moderator = moderator();
        Organization pending = organizations.save(new Organization("Alpha Company", OrgType.EXTERNAL_COMPANY));
        organizations.save(new Organization("Office", OrgType.UPRM_OFFICE));

        MvcResult result = mockMvc.perform(get("/api/admin/organizations/pending")
                        .session(login(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(pending.getId().toString()))
                .andExpect(jsonPath("$.items[0].name").value("Alpha Company"))
                .andExpect(jsonPath("$.items[0].approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("decisionReason", "email", "password", "orgType", "Office");
        assertThat(auditEvents.count()).isZero();
    }

    @Test
    void approvedRejectedAndInstitutionalOrganizationsAreExcluded() throws Exception {
        User moderator = moderator();
        Organization pending = organizations.save(new Organization("Pending", OrgType.EXTERNAL_COMPANY));
        Organization approved = new Organization("Approved", OrgType.EXTERNAL_COMPANY);
        approved.setApprovalStatus(ApprovalStatus.APPROVED);
        organizations.save(approved);
        Organization rejected = new Organization("Rejected", OrgType.EXTERNAL_COMPANY);
        rejected.setApprovalStatus(ApprovalStatus.REJECTED);
        organizations.save(rejected);
        organizations.save(new Organization("Department", OrgType.UPRM_DEPARTMENT));

        mockMvc.perform(get("/api/admin/organizations/pending").session(login(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(pending.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
        assertThat(auditEvents.count()).isZero();
    }

    @Test
    void emptyQueueHasEmptyItemsAndZeroTotals() throws Exception {
        mockMvc.perform(get("/api/admin/organizations/pending").session(login(moderator())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void pagesUseNameThenUniqueIdAndEndWithAnEmptyPage() throws Exception {
        MockHttpSession session = login(moderator());
        Organization first = organizations.save(new Organization("Alpha", OrgType.EXTERNAL_COMPANY));
        Organization second = organizations.save(new Organization("Alpha", OrgType.EXTERNAL_COMPANY));
        Organization third = organizations.save(new Organization("Beta", OrgType.EXTERNAL_COMPANY));
        List<UUID> alphaIds = List.of(first.getId(), second.getId()).stream()
                .sorted(Comparator.comparing(UUID::toString)).toList();

        mockMvc.perform(get("/api/admin/organizations/pending?page=0&size=1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(alphaIds.get(0).toString()))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
        mockMvc.perform(get("/api/admin/organizations/pending?page=1&size=1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(alphaIds.get(1).toString()));
        mockMvc.perform(get("/api/admin/organizations/pending?page=2&size=1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(third.getId().toString()));
        mockMvc.perform(get("/api/admin/organizations/pending?page=3&size=1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void invalidPageAndSizeReturnBadRequest() throws Exception {
        MockHttpSession session = login(moderator());
        for (String query : List.of("page=-1", "size=0", "size=101", "page=not-a-number")) {
            mockMvc.perform(get("/api/admin/organizations/pending?" + query).session(session))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/admin/organizations/pending?size=100").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void responseContainsOnlyDeliberateQueueFields() throws Exception {
        organizations.save(new Organization("Minimal Company", OrgType.EXTERNAL_COMPANY));
        MvcResult result = mockMvc.perform(get("/api/admin/organizations/pending")
                        .session(login(moderator())))
                .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        Map<String, Object> page = JsonPath.parse(body).read("$");
        Map<String, Object> item = JsonPath.parse(body).read("$.items[0]");
        assertThat(page.keySet()).containsExactlyInAnyOrder(
                "items", "page", "size", "totalElements", "totalPages");
        assertThat(item.keySet()).containsExactlyInAnyOrder("id", "name", "approvalStatus");
    }

    @Test
    void anonymousAndNonModeratorRequestsAreDenied() throws Exception {
        organizations.save(new Organization("Protected Company", OrgType.EXTERNAL_COMPANY));
        mockMvc.perform(get("/api/admin/organizations/pending"))
                .andExpect(status().isUnauthorized());
        User member = createUser("queue.member@upr.edu");
        mockMvc.perform(get("/api/admin/organizations/pending").session(login(member)))
                .andExpect(status().isForbidden());
        assertThat(auditEvents.count()).isZero();
    }

    @Test
    void suspendedOrRevokedModeratorLosesQueueAccessUsingSameSession() throws Exception {
        User moderator = moderator();
        MockHttpSession session = login(moderator);
        moderator.setStatus(UserStatus.SUSPENDED);
        users.saveAndFlush(moderator);
        mockMvc.perform(get("/api/admin/organizations/pending").session(session))
                .andExpect(status().isForbidden());
        moderator.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(moderator);
        roles.deleteAll();
        roles.flush();
        mockMvc.perform(get("/api/admin/organizations/pending").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void queuedCompanyDisappearsAfterRealDecisionAndProducesAuditRecord() throws Exception {
        User moderator = moderator();
        MockHttpSession session = login(moderator);
        Organization company = organizations.save(new Organization("Decision Company", OrgType.EXTERNAL_COMPANY));
        MvcResult queued = mockMvc.perform(get("/api/admin/organizations/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn();
        String returnedId = JsonPath.parse(queued.getResponse().getContentAsString()).read("$.items[0].id");
        assertThat(returnedId).isEqualTo(company.getId().toString());
        assertThat(auditEvents.count()).isZero();
        Instant started = Instant.now();

        mockMvc.perform(post("/api/admin/organizations/{id}/decision", returnedId)
                        .session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/organizations/pending").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(organizations.findById(company.getId()).orElseThrow().getApprovalStatus())
                .isEqualTo(ApprovalStatus.APPROVED);
        assertThat(auditEvents.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getActorId()).isEqualTo(moderator.getId());
            assertThat(event.getAction()).isEqualTo(AuditAction.ORGANIZATION_APPROVED);
            assertThat(event.getTargetType()).isEqualTo(AuditTargetType.ORGANIZATION);
            assertThat(event.getTargetId()).isEqualTo(company.getId());
            assertThat(event.getOccurredAt()).isBetween(started, Instant.now());
        });
    }

    @Test
    void queueEndpointAppearsInGeneratedOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/admin/organizations/pending'].get").exists())
                .andExpect(jsonPath("$.components.schemas.PendingCompanyResponse.properties.id").exists())
                .andExpect(jsonPath("$.components.schemas.PendingCompanyResponse.properties.name").exists())
                .andExpect(jsonPath("$.components.schemas.PendingCompanyResponse.properties.approvalStatus").exists());
    }

    private User createUser(String email) {
        User user = new User(email, passwordEncoder.encode(PASSWORD), AccountType.UPRM_MEMBER);
        user.setEmailVerified(true);
        return users.save(user);
    }

    private User moderator() {
        User user = new User("queue.moderator@upr.edu", passwordEncoder.encode(PASSWORD), AccountType.UPRM_MEMBER);
        user.setEmailVerified(true);
        user = users.save(user);
        roles.save(new RoleAssignment(user, Role.MODERATOR, null));
        return user;
    }

    private MockHttpSession login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}

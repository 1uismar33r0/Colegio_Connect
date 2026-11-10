package com.colegioconnect.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.audit.AuditEventRepository;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

/**
 * Reusable persisted fixtures and request helpers for authorization-denial
 * integration tests. Concrete test classes provide their Spring test annotations.
 */
public abstract class NegativeAuthorizationTestHarness {

    protected static final String TEST_PASSWORD = "a long negative authorization password";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected EmailCodeRepository emailCodeRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected RoleAssignmentRepository roleAssignmentRepository;
    @Autowired protected OrganizationRepository organizationRepository;
    @Autowired protected AuthorizationPolicy authorizationPolicy;
    @Autowired protected AuditEventRepository auditEventRepository;

    @BeforeEach
    protected void cleanAuthorizationFixtures() {
        auditEventRepository.deleteAll();
        roleAssignmentRepository.deleteAll();
        organizationRepository.deleteAll();
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User activeVerifiedUser(String email, Role... roles) {
        return user(email, UserStatus.ACTIVE, true, roles);
    }

    protected User user(UserStatus status) {
        return user(UUID.randomUUID() + "@upr.edu", status, true);
    }

    protected User user(String email, UserStatus status, boolean verified, Role... roles) {
        User user = new User(email, passwordEncoder.encode(TEST_PASSWORD), AccountType.UPRM_MEMBER);
        user.setStatus(status);
        user.setEmailVerified(verified);
        User saved = userRepository.save(user);
        for (Role role : roles) {
            assign(saved, role, null);
        }
        return saved;
    }

    protected Organization organization(ApprovalStatus status) {
        return organization("Test org " + UUID.randomUUID(), OrgType.UPRM_DEPARTMENT, status);
    }

    protected Organization organization(String name, OrgType type, ApprovalStatus status) {
        Organization organization = new Organization(name, type);
        organization.setApprovalStatus(status);
        return organizationRepository.save(organization);
    }

    protected RoleAssignment assign(User user, Role role, Organization organization) {
        return roleAssignmentRepository.save(new RoleAssignment(user, role, organization));
    }

    protected Authentication authenticationFor(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(
                user.getId().toString(), null, List.of());
    }

    protected MockHttpSession login(User user, ResultMatcher expectedStatus) throws Exception {
        return login(user.getEmail(), TEST_PASSWORD, expectedStatus);
    }

    protected MockHttpSession login(String email, String password, ResultMatcher expectedStatus)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(expectedStatus)
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected static String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}

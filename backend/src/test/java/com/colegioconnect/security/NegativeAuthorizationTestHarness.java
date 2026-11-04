package com.colegioconnect.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

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
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

/**
 * Reusable persisted fixtures for authorization-denial integration tests.
 * Concrete test classes provide their preferred Spring Boot test annotations.
 */
abstract class NegativeAuthorizationTestHarness {

    protected static final String TEST_PASSWORD = "a long negative authorization password";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RoleAssignmentRepository roleAssignmentRepository;
    @Autowired protected OrganizationRepository organizationRepository;
    @Autowired protected AuthorizationPolicy authorizationPolicy;
    @Autowired protected EmailCodeRepository emailCodeRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanAuthorizationFixtures() {
        roleAssignmentRepository.deleteAll();
        organizationRepository.deleteAll();
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User activeVerifiedUser(String email, Role... roles) {
        return user(email, UserStatus.ACTIVE, true, roles);
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

    protected Organization organization(String name, OrgType type, ApprovalStatus status) {
        Organization organization = organizationRepository.save(new Organization(name, type));
        organization.setApprovalStatus(status);
        return organizationRepository.save(organization);
    }

    protected RoleAssignment assign(User user, Role role, Organization organization) {
        return roleAssignmentRepository.save(new RoleAssignment(user, role, organization));
    }

    protected Authentication authenticationFor(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(
                user.getId().toString(), null, Set.of());
    }

    protected MockHttpSession login(User user) throws Exception {
        return login(user, status().isOk());
    }

    protected MockHttpSession login(User user, ResultMatcher expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail()
                                + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(expectedStatus)
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}

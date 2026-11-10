package com.colegioconnect.roles;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
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
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditActorType;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminBootstrapIntegrationTest extends NegativeAuthorizationTestHarness {

    @Autowired private AdminBootstrapService bootstrap;
    @Autowired private RoleAdminStateRepository state;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void resetBootstrapMarker() {
        jdbc.update("UPDATE role_admin_state SET bootstrap_completed_at = NULL, "
                + "bootstrap_user_id = NULL WHERE id = 1");
    }

    @Test
    void absentAndInvalidConfigurationDoesNotGrantAnyRole() {
        User intended = activeVerifiedUser("bootstrap@upr.edu");
        bootstrap.bootstrap(null);
        bootstrap.bootstrap("  ");
        bootstrap.bootstrap("bad search");
        bootstrap.bootstrap("unknown@upr.edu");
        assertThat(roleAssignmentRepository.findByUserId(intended.getId())).isEmpty();
        assertThat(auditEventRepository.count()).isZero();
        assertThat(state.bootstrapCompleted()).isFalse();
    }

    @Test
    void oneEligibleExistingAccountGetsOneAuditedGrantAndNeverRegainsItAfterRevocation() {
        User intended = activeVerifiedUser("bootstrap@upr.edu");
        User other = activeVerifiedUser("other@upr.edu");
        bootstrap.bootstrap(" BOOTSTRAP@UPR.EDU ");
        bootstrap.bootstrap("bootstrap@upr.edu");

        assertThat(roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
                intended.getId(), Role.MODERATOR)).isTrue();
        assertThat(roleAssignmentRepository.findByUserId(other.getId())).isEmpty();
        assertThat(state.bootstrapCompleted()).isTrue();
        assertThat(auditEventRepository.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getActorType()).isEqualTo(AuditActorType.SYSTEM);
            assertThat(event.getActorId()).isNull();
            assertThat(event.getAction()).isEqualTo(AuditAction.ROLE_ASSIGNED);
            assertThat(event.getTargetId()).isEqualTo(intended.getId());
            assertThat(event.getOccurredAt()).isNotNull();
        });

        roleAssignmentRepository.deleteAll();
        bootstrap.bootstrap("bootstrap@upr.edu");
        assertThat(roleAssignmentRepository.findByUserId(intended.getId())).isEmpty();
        assertThat(auditEventRepository.count()).isEqualTo(1);
    }

    @Test
    void ineligibleAccountsNeverReceiveModeratorRole() {
        User unverified = user("unverified@upr.edu", UserStatus.ACTIVE, false);
        bootstrap.bootstrap(unverified.getEmail());
        User suspended = user("suspended@upr.edu", UserStatus.SUSPENDED, true);
        bootstrap.bootstrap(suspended.getEmail());
        User external = new User("external@example.com", passwordEncoder.encode(TEST_PASSWORD),
                AccountType.EXTERNAL_ORGANIZATION);
        external.setEmailVerified(true);
        userRepository.save(external);
        bootstrap.bootstrap(external.getEmail());
        assertThat(roleAssignmentRepository.count()).isZero();
        assertThat(auditEventRepository.count()).isZero();
        assertThat(state.bootstrapCompleted()).isFalse();
    }

    @Test
    void existingEligibleModeratorCompletesBootstrapWithoutAnotherGrant() {
        activeVerifiedUser("existing@upr.edu", Role.MODERATOR);
        User intended = activeVerifiedUser("intended@upr.edu");
        bootstrap.bootstrap(intended.getEmail());
        assertThat(state.bootstrapCompleted()).isTrue();
        assertThat(roleAssignmentRepository.findByUserId(intended.getId())).isEmpty();
        assertThat(auditEventRepository.count()).isZero();
        roleAssignmentRepository.deleteAll();
        bootstrap.bootstrap(intended.getEmail());
        assertThat(roleAssignmentRepository.findByUserId(intended.getId())).isEmpty();
    }

    @Test
    void concurrentStartupGrantsExactlyOnce() throws Exception {
        User intended = activeVerifiedUser("concurrent@upr.edu");
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                bootstrap.bootstrap(intended.getEmail());
                return true;
            });
            var second = pool.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                bootstrap.bootstrap(intended.getEmail());
                return true;
            });
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS))).containsExactly(true, true);
        }
        assertThat(roleAssignmentRepository.findByUserId(intended.getId())).hasSize(1);
        assertThat(auditEventRepository.count()).isEqualTo(1);
    }
}

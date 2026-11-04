package com.colegioconnect.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.repository.EmailCodeRepository;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuditServiceIntegrationTest {

    @Autowired private AuditService auditService;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailCodeRepository emailCodeRepository;
    @Autowired private RoleAssignmentRepository roleAssignmentRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanDatabase() {
        auditEventRepository.deleteAll();
        roleAssignmentRepository.deleteAll();
        emailCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void committedAdministrativeActionPersistsAuditEvent() {
        User actor = createActor();
        UUID targetId = UUID.randomUUID();
        Instant startedAt = Instant.now();

        transactionTemplate.executeWithoutResult(status -> auditService.record(
                actor.getId(), AuditAction.ROLE_ASSIGNED, AuditTargetType.USER, targetId));

        assertThat(auditEventRepository.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getActorId()).isEqualTo(actor.getId());
            assertThat(event.getAction()).isEqualTo(AuditAction.ROLE_ASSIGNED);
            assertThat(event.getTargetType()).isEqualTo(AuditTargetType.USER);
            assertThat(event.getTargetId()).isEqualTo(targetId);
            assertThat(event.getOccurredAt()).isAfterOrEqualTo(startedAt);
        });
    }

    @Test
    void failedAdministrativeActionRollsBackAuditEvent() {
        User actor = createActor();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            auditService.record(actor.getId(), AuditAction.ROLE_REVOKED,
                    AuditTargetType.USER, UUID.randomUUID());
            throw new IllegalStateException("simulated business failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void recordingOutsideBusinessTransactionIsRejected() {
        User actor = createActor();

        assertThatThrownBy(() -> auditService.record(actor.getId(), AuditAction.ORGANIZATION_APPROVED,
                AuditTargetType.ORGANIZATION, UUID.randomUUID()))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(auditEventRepository.count()).isZero();
    }

    private User createActor() {
        User actor = new User("audit.actor@upr.edu", "not-a-real-password-hash", AccountType.UPRM_MEMBER);
        actor.setEmailVerified(true);
        return userRepository.save(actor);
    }
}

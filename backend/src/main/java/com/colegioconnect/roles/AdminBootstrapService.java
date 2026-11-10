package com.colegioconnect.roles;

import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditService;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@Service
public class AdminBootstrapService {

    private final UserRepository users;
    private final RoleAssignmentRepository assignments;
    private final RoleAdminStateRepository state;
    private final AuditService audit;

    public AdminBootstrapService(UserRepository users, RoleAssignmentRepository assignments,
                                 RoleAdminStateRepository state, AuditService audit) {
        this.users = users;
        this.assignments = assignments;
        this.state = state;
        this.audit = audit;
    }

    @Transactional
    public void bootstrap(String configuredEmail) {
        if (configuredEmail == null || configuredEmail.isBlank()) {
            return;
        }
        String email = configuredEmail.strip().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !email.matches("[^@\\s]+@[^@\\s]+")) {
            return;
        }
        state.lock();
        if (state.bootstrapCompleted()) {
            return;
        }
        // An existing eligible moderator owns initialization. Mark it durable so that
        // a later revocation is not reversed by another startup.
        if (assignments.countEligibleGlobalModerators(Role.MODERATOR, UserStatus.ACTIVE,
                AccountType.UPRM_MEMBER) > 0) {
            state.completeBootstrap(null);
            return;
        }
        var matches = users.findByNormalizedEmail(email, PageRequest.of(0, 2));
        if (matches.getTotalElements() != 1) {
            return;
        }
        User user = matches.getContent().getFirst();
        if (user.getAccountType() != AccountType.UPRM_MEMBER || user.getStatus() != UserStatus.ACTIVE
                || !user.isEmailVerified()) {
            return;
        }
        assignments.saveAndFlush(new RoleAssignment(user, Role.MODERATOR, null));
        audit.recordSystem(AuditAction.ROLE_ASSIGNED, AuditTargetType.USER, user.getId());
        state.completeBootstrap(user.getId());
    }
}

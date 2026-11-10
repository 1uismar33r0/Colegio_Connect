package com.colegioconnect.organizations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditService;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

/**
 * Creates the first moderator at startup, without any public API.
 *
 * How to use: register and verify an @upr.edu account normally, then start the backend
 * with APP_BOOTSTRAP_ADMIN_EMAIL set to that email. It only works while no moderator
 * exists, so the variable cannot be used later as a back door.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final String adminEmail;
    private final UserRepository users;
    private final RoleAssignmentRepository roles;
    private final AuditService auditService;

    public AdminBootstrap(@Value("${app.bootstrap.admin-email:}") String adminEmail,
                          UserRepository users, RoleAssignmentRepository roles, AuditService auditService) {
        this.adminEmail = adminEmail;
        this.users = users;
        this.roles = roles;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank()) {
            return; // not configured: do nothing
        }
        if (roles.existsByRole(Role.MODERATOR)) {
            log.info("Admin bootstrap skipped: a moderator already exists.");
            return;
        }

        User user = users.findByEmail(adminEmail.trim().toLowerCase()).orElse(null);
        if (user == null || user.getAccountType() != AccountType.UPRM_MEMBER || !user.isEmailVerified()) {
            // The email is not logged on purpose
            log.warn("Admin bootstrap skipped: the configured account is missing or not a verified UPRM member.");
            return;
        }

        roles.save(new RoleAssignment(user, Role.MODERATOR, null));
        // No other admin exists yet, so the user is recorded as their own actor
        auditService.record(user.getId(), AuditAction.ROLE_ASSIGNED, AuditTargetType.USER, user.getId());
        log.info("Admin bootstrap: moderator role granted to the configured account.");
    }
}
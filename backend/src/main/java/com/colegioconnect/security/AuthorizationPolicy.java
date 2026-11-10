package com.colegioconnect.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

// Shared authorization checks, used from @PreAuthorize as @authz.methodName(...)
@Component("authz")
public class AuthorizationPolicy {

    private final UserRepository userRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final OrganizationRepository organizationRepository;

    public AuthorizationPolicy(UserRepository userRepository,
                               RoleAssignmentRepository roleAssignmentRepository,
                               OrganizationRepository organizationRepository) {
        this.userRepository = userRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.organizationRepository = organizationRepository;
    }

    // The account still exists and is not suspended (checked on every request, not only at login)
    public boolean isActive(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return userRepository.findById(userId)
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }

    // The user has this role inside this specific organization
    public boolean hasRoleIn(Authentication authentication, Role role, UUID organizationId) {
        UUID userId = UUID.fromString(authentication.getName());
        return roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationId(userId, role, organizationId);
    }

    // Moderation is a platform role and must be rechecked from persistence on every protected action.
    public boolean canModerate(Authentication authentication) {
        if (!isActive(authentication)) {
            return false;
        }
        UUID userId = UUID.fromString(authentication.getName());
        return roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(userId, Role.MODERATOR);
    }

    // An administrator approved this organization
    public boolean isApproved(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .map(organization -> organization.getApprovalStatus() == ApprovalStatus.APPROVED)
                .orElse(false);
    }
}

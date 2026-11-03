package com.colegioconnect.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

/**
 * Reusable server-side authorization checks for authenticated Colegio Connect users.
 *
 * <p>These checks query persisted state so a suspended account, revoked role, or
 * changed organization approval takes effect for an existing session.</p>
 */
@Component("authorizationPolicy")
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

    public boolean isActive(Authentication authentication) {
        return userId(authentication)
                .flatMap(userRepository::findById)
                .map(user -> user.getStatus() == UserStatus.ACTIVE && user.isEmailVerified())
                .orElse(false);
    }

    public boolean hasRole(Authentication authentication, Role role) {
        return activeUserId(authentication)
                .map(userId -> roleAssignmentRepository.existsByUserIdAndRole(userId, role))
                .orElse(false);
    }

    public boolean isOwner(Authentication authentication, UUID ownerId) {
        return activeUserId(authentication)
                .map(ownerId::equals)
                .orElse(false);
    }

    public boolean isOrganizationMember(Authentication authentication, UUID organizationId) {
        return activeUserId(authentication)
                .map(userId -> roleAssignmentRepository.existsByUserIdAndOrganizationId(userId, organizationId))
                .orElse(false);
    }

    public boolean hasApprovedOrganizationMembership(Authentication authentication, UUID organizationId) {
        return isOrganizationMember(authentication, organizationId)
                && organizationRepository.existsByIdAndApprovalStatus(organizationId, ApprovalStatus.APPROVED);
    }

    public boolean canPublishForOrganization(Authentication authentication, UUID organizationId) {
        return activeUserId(authentication)
                .flatMap(userId -> organizationRepository.findById(organizationId)
                        .map(organization -> canPublishForOrganization(userId, organizationId,
                                organization.getOrgType(), organization.getApprovalStatus())))
                .orElse(false);
    }

    private boolean canPublishForOrganization(UUID userId, UUID organizationId,
                                               OrgType organizationType, ApprovalStatus approvalStatus) {
        return switch (organizationType) {
            case UPRM_OFFICE, UPRM_DEPARTMENT -> roleAssignmentRepository
                    .existsByUserIdAndOrganizationIdAndRole(userId, organizationId, Role.FACULTY_OFFICE_DEPT);
            case EXTERNAL_COMPANY -> approvalStatus == ApprovalStatus.APPROVED
                    && roleAssignmentRepository.existsByUserIdAndOrganizationIdAndRole(
                            userId, organizationId, Role.EXTERNAL_COMPANY);
        };
    }

    public boolean canModerate(Authentication authentication) {
        return hasRole(authentication, Role.ADMIN_MODERATOR);
    }

    public boolean canAssignRoles(Authentication authentication) {
        return hasRole(authentication, Role.ADMIN_MODERATOR);
    }

    private Optional<UUID> activeUserId(Authentication authentication) {
        return isActive(authentication) ? userId(authentication) : Optional.empty();
    }

    private Optional<UUID> userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}

package com.colegioconnect.roles;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditService;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@Service
public class RoleAdminService {

    private final UserRepository users;
    private final RoleAssignmentRepository assignments;
    private final OrganizationRepository organizations;
    private final RoleAdminStateRepository state;
    private final AuditService audit;

    public RoleAdminService(UserRepository users, RoleAssignmentRepository assignments,
                            OrganizationRepository organizations, RoleAdminStateRepository state,
                            AuditService audit) {
        this.users = users;
        this.assignments = assignments;
        this.organizations = organizations;
        this.state = state;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AdminUserPageResponse search(String email, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw badRequest("page must be non-negative and size must be between 1 and 100");
        }
        String normalized = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !normalized.matches("[^@\\s]+@[^@\\s]+")) {
            throw badRequest("A complete email address is required.");
        }
        Page<User> result = users.findByNormalizedEmail(normalized,
                PageRequest.of(page, size, Sort.by("email", "id")));
        List<UUID> ids = result.getContent().stream().map(User::getId).toList();
        Map<UUID, List<RoleAssignment>> byUser = ids.isEmpty() ? Map.of()
                : assignments.findWithOrganizationByUserIdIn(ids).stream()
                        .collect(Collectors.groupingBy(value -> value.getUser().getId()));
        List<AdminUserResponse> items = result.getContent().stream()
                .map(user -> AdminUserResponse.from(user, byUser.getOrDefault(user.getId(), List.of())))
                .toList();
        return new AdminUserPageResponse(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public void assign(UUID actorId, UUID userId, RoleChangeRequest request) {
        validateShape(request);
        state.lock();
        requireCurrentModerator(actorId);
        User user = user(userId);
        Organization organization = resolveOrganization(request.organizationId());
        validateEligibility(user, request.role(), organization);
        if (existing(userId, request.role(), request.organizationId()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already assigned.");
        }
        assignments.saveAndFlush(new RoleAssignment(user, request.role(), organization));
        audit.record(actorId, AuditAction.ROLE_ASSIGNED, AuditTargetType.USER, userId);
    }

    @Transactional
    public void revoke(UUID actorId, UUID userId, RoleChangeRequest request) {
        validateShape(request);
        state.lock();
        requireCurrentModerator(actorId);
        user(userId);
        RoleAssignment assignment = existing(userId, request.role(), request.organizationId());
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role assignment not found.");
        }
        if (request.role() == Role.MODERATOR) {
            if (actorId.equals(userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A moderator cannot revoke their own moderator role.");
            }
            long eligible = assignments.countEligibleGlobalModerators(
                    Role.MODERATOR, UserStatus.ACTIVE, AccountType.UPRM_MEMBER);
            if (eligible <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot remove the last active moderator.");
            }
        }
        assignments.delete(assignment);
        assignments.flush();
        audit.record(actorId, AuditAction.ROLE_REVOKED, AuditTargetType.USER, userId);
    }

    private RoleAssignment existing(UUID userId, Role role, UUID organizationId) {
        return organizationId == null
                ? assignments.findByUserIdAndRoleAndOrganizationIsNull(userId, role).orElse(null)
                : assignments.findByUserIdAndRoleAndOrganizationId(userId, role, organizationId)
                        .orElse(null);
    }

    private void requireCurrentModerator(UUID actorId) {
        boolean eligible = users.findById(actorId)
                .filter(actor -> actor.getStatus() == UserStatus.ACTIVE && actor.isEmailVerified()
                        && actor.getAccountType() == AccountType.UPRM_MEMBER)
                .isPresent();
        if (!eligible || !assignments.existsByUserIdAndRoleAndOrganizationIsNull(
                actorId, Role.MODERATOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Moderator authority is no longer current.");
        }
    }

    private User user(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private Organization resolveOrganization(UUID id) {
        if (id == null) {
            return null;
        }
        return organizations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Organization not found."));
    }

    private void validateShape(RoleChangeRequest request) {
        if (request == null || request.role() == null) {
            throw badRequest("Choose a role.");
        }
        boolean global = request.role() == Role.STUDENT || request.role() == Role.MODERATOR;
        if ((global && request.organizationId() != null)
                || (!global && request.organizationId() == null)) {
            throw badRequest(global ? "Global roles cannot have an organization."
                    : "An organization is required for this role.");
        }
    }

    private void validateEligibility(User user, Role role, Organization organization) {
        if (user.getStatus() != UserStatus.ACTIVE || !user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The account must be active and verified before receiving a role.");
        }
        if (role == Role.COMPANY) {
            if (user.getAccountType() != AccountType.EXTERNAL_ORGANIZATION
                    || organization.getOrgType() != OrgType.EXTERNAL_COMPANY) {
                throw badRequest("Company roles require an external account and company.");
            }
            if (organization.getApprovalStatus() != ApprovalStatus.APPROVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "The company must be approved before assigning its role.");
            }
        } else if (user.getAccountType() != AccountType.UPRM_MEMBER) {
            throw badRequest("This role requires a UPRM member account.");
        } else if (role == Role.INSTITUTIONAL
                && organization.getOrgType() != OrgType.UPRM_OFFICE
                && organization.getOrgType() != OrgType.UPRM_DEPARTMENT) {
            throw badRequest("Institutional roles require a UPRM office or department.");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

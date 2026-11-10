package com.colegioconnect.organizations;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditService;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@Service
public class RoleAssignmentService {

    private final RoleAssignmentRepository roles;
    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final AuditService auditService;

    public RoleAssignmentService(RoleAssignmentRepository roles, UserRepository users,
                                 OrganizationRepository organizations, AuditService auditService) {
        this.roles = roles;
        this.users = users;
        this.organizations = organizations;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleAssignmentResponse> findForUser(UUID userId) {
        return roles.findByUserId(userId).stream().map(RoleAssignmentResponse::from).toList();
    }

    @Transactional
    public RoleAssignmentResponse assign(UUID actorId, RoleAssignmentRequest request) {
        User user = users.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        // Proposal: only UPRM_MEMBER accounts can hold faculty or moderator roles
        if (user.getAccountType() != AccountType.UPRM_MEMBER || !user.isEmailVerified()) {
            throw badRequest("Only verified UPRM members can receive this role.");
        }

        Organization organization = switch (request.role()) {
            case MODERATOR -> {
                if (request.organizationId() != null) {
                    throw badRequest("The moderator role does not use an organization.");
                }
                yield null;
            }
            case INSTITUTIONAL -> uprmOrganization(request.organizationId());
            default -> throw badRequest("Only INSTITUTIONAL and MODERATOR roles can be assigned here.");
        };

        if (alreadyHasRole(user.getId(), request.role(), organization)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The user already has this role.");
        }

        RoleAssignment saved = roles.save(new RoleAssignment(user, request.role(), organization));
        // Same transaction: if the audit write fails, the role is not saved either
        auditService.record(actorId, AuditAction.ROLE_ASSIGNED, AuditTargetType.USER, user.getId());
        return RoleAssignmentResponse.from(saved);
    }

    @Transactional
    public void revoke(UUID actorId, UUID assignmentId) {
        RoleAssignment assignment = roles.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role assignment not found."));

        if (assignment.getRole() != Role.INSTITUTIONAL && assignment.getRole() != Role.MODERATOR) {
            throw badRequest("Only INSTITUTIONAL and MODERATOR roles can be revoked here.");
        }

        UUID userId = assignment.getUser().getId();
        // Prevents a moderator from locking themselves (and maybe everyone) out by mistake
        if (assignment.getRole() == Role.MODERATOR && userId.equals(actorId)) {
            throw badRequest("You cannot remove your own moderator role.");
        }

        roles.delete(assignment);
        auditService.record(actorId, AuditAction.ROLE_REVOKED, AuditTargetType.USER, userId);
    }

    private Organization uprmOrganization(UUID organizationId) {
        if (organizationId == null) {
            throw badRequest("The institutional role needs an organization.");
        }
        Organization organization = organizations.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found."));
        if (organization.getOrgType() == OrgType.EXTERNAL_COMPANY) {
            throw badRequest("The institutional role needs a UPRM office or department.");
        }
        return organization;
    }

    private boolean alreadyHasRole(UUID userId, Role role, Organization organization) {
        return organization == null
                ? roles.existsByUserIdAndRoleAndOrganizationIsNull(userId, role)
                : roles.existsByUserIdAndRoleAndOrganizationId(userId, role, organization.getId());
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
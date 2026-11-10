package com.colegioconnect.listings;

import java.util.UUID;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.accounts.repository.UserRepository;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.repository.OrganizationRepository;
import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@Component
public class DashboardAccess {

    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final RoleAssignmentRepository roles;
    private final JdbcTemplate jdbc;

    public DashboardAccess(UserRepository users, OrganizationRepository organizations,
                           RoleAssignmentRepository roles, JdbcTemplate jdbc) {
        this.users = users;
        this.organizations = organizations;
        this.roles = roles;
        this.jdbc = jdbc;
    }

    public Organization requireOffice(UUID actorId, UUID organizationId) {
        Organization organization = require(actorId, organizationId,
                AccountType.UPRM_MEMBER, Role.INSTITUTIONAL);
        if (organization.getOrgType() != OrgType.UPRM_OFFICE
                && organization.getOrgType() != OrgType.UPRM_DEPARTMENT) {
            throw denied();
        }
        return organization;
    }

    public UUID resolveOfficeId(UUID actorId, UUID selectedId) {
        return resolveOffice(actorId, selectedId);
    }

    public Organization companyForDashboard(UUID actorId, UUID selectedId) {
        boolean active = users.findById(actorId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE
                        && user.isEmailVerified()
                        && user.getAccountType() == AccountType.EXTERNAL_ORGANIZATION)
                .isPresent();
        if (!active) throw denied();
        List<UUID> approved = roles.findWithOrganizationByUserIdIn(List.of(actorId)).stream()
                .filter(assignment -> assignment.getRole() == Role.COMPANY)
                .map(RoleAssignment::getOrganization)
                .filter(org -> org != null && org.getOrgType() == OrgType.EXTERNAL_COMPANY
                        && org.getApprovalStatus() == ApprovalStatus.APPROVED)
                .map(Organization::getId).toList();
        List<UUID> pending = jdbc.queryForList("""
                SELECT owner.organization_id FROM company_application_owners owner
                  JOIN organizations org ON org.id = owner.organization_id
                 WHERE owner.user_id = ? AND org.org_type = 'EXTERNAL_COMPANY'
                   AND org.approval_status = 'PENDING'
                """, UUID.class, actorId);
        List<UUID> eligible = java.util.stream.Stream.concat(approved.stream(), pending.stream())
                .distinct().toList();
        if (selectedId == null) {
            if (eligible.isEmpty()) throw denied();
            if (eligible.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Select an organizationId for this dashboard.");
            selectedId = eligible.getFirst();
        }
        if (!eligible.contains(selectedId)) throw denied();
        return organizations.findById(selectedId).orElseThrow(this::denied);
    }

    private UUID resolveOffice(UUID actorId, UUID selectedId) {
        if (selectedId != null) {
            return selectedId;
        }
        boolean active = users.findById(actorId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE
                        && user.isEmailVerified()
                        && user.getAccountType() == AccountType.UPRM_MEMBER)
                .isPresent();
        if (!active) {
            throw denied();
        }
        List<UUID> eligible = roles.findWithOrganizationByUserIdIn(List.of(actorId)).stream()
                .filter(assignment -> assignment.getRole() == Role.INSTITUTIONAL)
                .map(RoleAssignment::getOrganization)
                .filter(organization -> organization != null
                        && (organization.getOrgType() == OrgType.UPRM_OFFICE
                            || organization.getOrgType() == OrgType.UPRM_DEPARTMENT))
                .map(Organization::getId).distinct().toList();
        if (eligible.isEmpty()) {
            throw denied();
        }
        if (eligible.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Select an organizationId for this dashboard.");
        }
        return eligible.getFirst();
    }

    private Organization require(UUID actorId, UUID organizationId,
                                 AccountType accountType, Role role) {
        boolean eligible = users.findById(actorId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE
                        && user.isEmailVerified()
                        && user.getAccountType() == accountType)
                .isPresent();
        if (!eligible || !roles.existsByUserIdAndRoleAndOrganizationId(
                actorId, role, organizationId)) {
            throw denied();
        }
        return organizations.findById(organizationId).orElseThrow(this::denied);
    }

    private ResponseStatusException denied() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No current membership in the requested organization.");
    }
}

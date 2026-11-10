package com.colegioconnect.organizations;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.audit.AuditAction;
import com.colegioconnect.audit.AuditService;
import com.colegioconnect.audit.AuditTargetType;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.repository.OrganizationRepository;

@Service
public class CompanyDecisionService {
    private final OrganizationRepository organizations;
    private final AuditService auditService;

    public CompanyDecisionService(OrganizationRepository organizations, AuditService auditService) {
        this.organizations = organizations;
        this.auditService = auditService;
    }

    @Transactional
    public void decide(UUID actorId, UUID organizationId, CompanyDecisionRequest request) {
        if (request == null || request.decision() == null || request.decision() == ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose approval or rejection.");
        }
        String reason = request.reason() == null ? null : request.reason().strip();
        if (reason != null && reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision reason is too long.");
        }
        if (request.decision() == ApprovalStatus.REJECTED && (reason == null || reason.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A rejection reason is required.");
        }
        if (reason != null && reason.isEmpty()) {
            reason = null;
        }

        Organization organization = organizations.findByIdForUpdate(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found."));
        if (organization.getOrgType() != OrgType.EXTERNAL_COMPANY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only external companies can be decided.");
        }
        if (organization.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The organization has already been decided.");
        }

        organization.setApprovalStatus(request.decision());
        organization.setDecisionReason(reason);
        auditService.record(actorId,
                request.decision() == ApprovalStatus.APPROVED
                        ? AuditAction.ORGANIZATION_APPROVED : AuditAction.ORGANIZATION_REJECTED,
                AuditTargetType.ORGANIZATION, organizationId);
    }
}

package com.colegioconnect.organizations;

import java.util.UUID;

import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.Organization;

public record PendingCompanyResponse(UUID id, String name, ApprovalStatus approvalStatus) {
    static PendingCompanyResponse from(Organization organization) {
        return new PendingCompanyResponse(
                organization.getId(), organization.getName(), organization.getApprovalStatus());
    }
}

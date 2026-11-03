package com.colegioconnect.organizations.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsByIdAndApprovalStatus(UUID id, ApprovalStatus approvalStatus);
}

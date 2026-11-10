package com.colegioconnect.organizations.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {
    List<RoleAssignment> findByUserId(UUID userId);
    boolean existsByUserIdAndRoleAndOrganizationId(UUID userId, Role role, UUID organizationId);
    boolean existsByUserIdAndRoleAndOrganizationIsNull(UUID userId, Role role);
}

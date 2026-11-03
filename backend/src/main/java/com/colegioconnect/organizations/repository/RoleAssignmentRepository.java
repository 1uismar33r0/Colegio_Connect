package com.colegioconnect.organizations.repository;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colegioconnect.organizations.entity.RoleAssignment;
import com.colegioconnect.organizations.entity.Role;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {

    List<RoleAssignment> findAllByUserId(UUID userId);

    boolean existsByUserIdAndRole(UUID userId, Role role);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    boolean existsByUserIdAndOrganizationIdAndRole(UUID userId, UUID organizationId, Role role);
}

package com.colegioconnect.organizations.repository;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {
    List<RoleAssignment> findByUserId(UUID userId);
    boolean existsByUserIdAndRoleAndOrganizationId(UUID userId, Role role, UUID organizationId);
    boolean existsByUserIdAndRoleAndOrganizationIsNull(UUID userId, Role role);
      boolean existsByRole(Role role);

    @Query("select assignment from RoleAssignment assignment left join fetch assignment.organization "
            + "where assignment.user.id in :userIds")
    List<RoleAssignment> findWithOrganizationByUserIdIn(@Param("userIds") Collection<UUID> userIds);
}

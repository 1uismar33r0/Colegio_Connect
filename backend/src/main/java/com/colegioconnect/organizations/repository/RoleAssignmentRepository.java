package com.colegioconnect.organizations.repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {
    List<RoleAssignment> findByUserId(UUID userId);
    boolean existsByUserIdAndRoleAndOrganizationId(UUID userId, Role role, UUID organizationId);
    boolean existsByUserIdAndRoleAndOrganizationIsNull(UUID userId, Role role);

    Optional<RoleAssignment> findByUserIdAndRoleAndOrganizationIsNull(UUID userId, Role role);

    Optional<RoleAssignment> findByUserIdAndRoleAndOrganizationId(
            UUID userId, Role role, UUID organizationId);

    @Query("select assignment from RoleAssignment assignment left join fetch assignment.organization "
            + "where assignment.user.id in :userIds")
    List<RoleAssignment> findWithOrganizationByUserIdIn(@Param("userIds") Collection<UUID> userIds);

    @Query("select count(assignment) from RoleAssignment assignment "
            + "where assignment.role = :role and assignment.organization is null "
            + "and assignment.user.status = :status "
            + "and assignment.user.accountType = :accountType "
            + "and assignment.user.emailVerified = true")
    long countEligibleGlobalModerators(@Param("role") Role role,
                                        @Param("status") UserStatus status,
                                        @Param("accountType") AccountType accountType);
}

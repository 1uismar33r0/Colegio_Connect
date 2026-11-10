package com.colegioconnect.roles;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.organizations.entity.RoleAssignment;

public record AdminUserResponse(UUID id, String email, AccountType accountType, UserStatus status,
                                List<Role> roles, List<RoleAssignmentResponse> roleAssignments) {

    static AdminUserResponse from(User user, List<RoleAssignment> assignments) {
        List<RoleAssignmentResponse> scoped = assignments.stream()
                .map(RoleAssignmentResponse::from)
                .sorted(Comparator.comparing((RoleAssignmentResponse value) -> value.role().name())
                        .thenComparing(value -> value.organizationId() == null
                                ? "" : value.organizationId().toString()))
                .toList();
        List<Role> distinct = scoped.stream().map(RoleAssignmentResponse::role)
                .distinct().sorted(Comparator.comparing(Role::name)).toList();
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getAccountType(),
                user.getStatus(), distinct, scoped);
    }
}

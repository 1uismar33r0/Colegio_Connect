package com.colegioconnect.security;

import java.util.Collection;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.colegioconnect.organizations.repository.RoleAssignmentRepository;

@Service
public class RoleAuthorityService {

    private final RoleAssignmentRepository roleAssignmentRepository;

    public RoleAuthorityService(RoleAssignmentRepository roleAssignmentRepository) {
        this.roleAssignmentRepository = roleAssignmentRepository;
    }

    public Collection<? extends GrantedAuthority> authoritiesFor(UUID userId) {
        return roleAssignmentRepository.findAllByUserId(userId).stream()
                .map(assignment -> new SimpleGrantedAuthority("ROLE_" + assignment.getRole().name()))
                .distinct()
                .toList();
    }
}

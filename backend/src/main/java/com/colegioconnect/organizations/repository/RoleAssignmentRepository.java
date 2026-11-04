package com.colegioconnect.organizations.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colegioconnect.organizations.entity.RoleAssignment;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {
}
package com.colegioconnect.organizations.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.colegioconnect.accounts.entity.User;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_assignments")
public class RoleAssignment {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization; // null when the role has no organization

    protected RoleAssignment() {
        // Required by JPA
    }

    public RoleAssignment(User user, Role role, Organization organization) {
        this.user = user;
        this.role = role;
        this.organization = organization;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Role getRole() { return role; }
    public Organization getOrganization() { return organization; }
}
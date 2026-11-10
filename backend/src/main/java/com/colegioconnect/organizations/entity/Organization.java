package com.colegioconnect.organizations.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private OrgType orgType;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    protected Organization() {
        // Required by JPA
    }

    public Organization(String name, OrgType orgType) {
        this.name = name;
        this.orgType = orgType;
        this.approvalStatus = ApprovalStatus.PENDING;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public OrgType getOrgType() { return orgType; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public String getDecisionReason() { return decisionReason; }

    public void setName(String name) { this.name = name; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
}

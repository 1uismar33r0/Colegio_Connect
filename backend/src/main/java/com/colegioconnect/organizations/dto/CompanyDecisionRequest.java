package com.colegioconnect.organizations.dto;

import com.colegioconnect.organizations.entity.ApprovalStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompanyDecisionRequest(@NotNull ApprovalStatus decision,
                                     @Size(max = 500) String reason) {
}

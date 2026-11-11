package com.colegioconnect.listings;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/company")
public class CompanyDashboardController {

    private final CompanyDashboardService dashboard;

    public CompanyDashboardController(CompanyDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Company dashboard for a selected approved organization")
    public CompanyDashboardResponse current(@RequestParam(required = false) UUID organizationId,
                                            @Parameter(hidden = true) Authentication authentication) {
        return dashboard.forOrganization(UUID.fromString(authentication.getName()), organizationId);
    }
}

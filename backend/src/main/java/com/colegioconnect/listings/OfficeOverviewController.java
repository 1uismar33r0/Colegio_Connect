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
@RequestMapping("/api/institution")
public class OfficeOverviewController {

    private final OfficeOverviewService overview;

    public OfficeOverviewController(OfficeOverviewService overview) {
        this.overview = overview;
    }

    @GetMapping("/overview")
    @Operation(summary = "Institutional overview for a selected authorized organization")
    public OfficeOverviewResponse current(@RequestParam(required = false) UUID organizationId,
                                          @Parameter(hidden = true) Authentication authentication) {
        return overview.forOrganization(UUID.fromString(authentication.getName()), organizationId);
    }
}

package com.colegioconnect.organizations;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/organizations")
public class CompanyDecisionController {
    private final CompanyDecisionService decisions;

    public CompanyDecisionController(CompanyDecisionService decisions) {
        this.decisions = decisions;
    }

    @PostMapping("/{id}/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.canModerate(authentication)")
    public void decide(@PathVariable UUID id, @Valid @RequestBody CompanyDecisionRequest request,
                       @Parameter(hidden = true) Authentication authentication) {
        decisions.decide(UUID.fromString(authentication.getName()), id, request);
    }
}

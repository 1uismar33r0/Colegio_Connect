package com.colegioconnect.organizations;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/role-assignments")
public class RoleAssignmentController {

    private final RoleAssignmentService service;

    public RoleAssignmentController(RoleAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.canModerate(authentication)")
    public List<RoleAssignmentResponse> findForUser(@RequestParam UUID userId) {
        return service.findForUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.canModerate(authentication)")
    public RoleAssignmentResponse assign(@Valid @RequestBody RoleAssignmentRequest request,
                                         @Parameter(hidden = true) Authentication authentication) {
        return service.assign(UUID.fromString(authentication.getName()), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.canModerate(authentication)")
    public void revoke(@PathVariable UUID id, @Parameter(hidden = true) Authentication authentication) {
        service.revoke(UUID.fromString(authentication.getName()), id);
    }
}
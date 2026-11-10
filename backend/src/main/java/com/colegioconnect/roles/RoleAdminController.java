package com.colegioconnect.roles;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
public class RoleAdminController {

    private final RoleAdminService service;

    public RoleAdminController(RoleAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.canModerate(authentication)")
    @Operation(summary = "Find a user by exact email for role administration")
    public AdminUserPageResponse search(@RequestParam String email,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.search(email, page, size);
    }

    @PostMapping("/{id}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.canModerate(authentication)")
    @Operation(summary = "Assign a global or organization-scoped role")
    public void assign(@PathVariable UUID id, @Valid @RequestBody RoleChangeRequest request,
                       @Parameter(hidden = true) Authentication authentication) {
        service.assign(UUID.fromString(authentication.getName()), id, request);
    }

    @DeleteMapping("/{id}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.canModerate(authentication)")
    @Operation(summary = "Revoke an exact global or organization-scoped role")
    public void revoke(@PathVariable UUID id, @Valid @RequestBody RoleChangeRequest request,
                       @Parameter(hidden = true) Authentication authentication) {
        service.revoke(UUID.fromString(authentication.getName()), id, request);
    }
}

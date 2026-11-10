package com.colegioconnect.organizations.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.colegioconnect.organizations.dto.PendingCompanyPageResponse;
import com.colegioconnect.organizations.service.PendingCompanyQueueService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/admin/organizations")
public class PendingCompanyQueueController {
    private final PendingCompanyQueueService queue;

    public PendingCompanyQueueController(PendingCompanyQueueService queue) {
        this.queue = queue;
    }

    @GetMapping("/pending")
    @PreAuthorize("@authz.canModerate(authentication)")
    @Operation(summary = "List pending external companies",
            description = "Returns minimal company review entries, ordered by name then ID; page size is 1 to 100.")
    public PendingCompanyPageResponse findPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queue.findPending(page, size);
    }
}

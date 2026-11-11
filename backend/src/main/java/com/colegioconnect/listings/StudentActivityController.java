package com.colegioconnect.listings;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/me")
public class StudentActivityController {

    private final StudentActivityService activity;

    public StudentActivityController(StudentActivityService activity) {
        this.activity = activity;
    }

    @GetMapping("/activity")
    @PreAuthorize("@authz.canStudent(authentication)")
    @Operation(summary = "Current student's saved and active response counts")
    public StudentActivityResponse current(@Parameter(hidden = true) Authentication authentication) {
        return activity.forUser(UUID.fromString(authentication.getName()));
    }
}

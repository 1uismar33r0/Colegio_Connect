package com.colegioconnect.listings;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/admin")
public class ReviewQueueController {

    private final ReviewQueueService queue;

    public ReviewQueueController(ReviewQueueService queue) {
        this.queue = queue;
    }

    @GetMapping("/review-queue")
    @PreAuthorize("@authz.canModerate(authentication)")
    @Operation(summary = "Review pending companies and reported listings",
            description = "The profile-verification count is null because that workflow "
                    + "does not exist in the approved implementation. Results are paged "
                    + "and ordered by persisted receipt time and ID.")
    public ReviewQueueResponse find(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return queue.find(page, size);
    }
}

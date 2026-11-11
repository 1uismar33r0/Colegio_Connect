package com.colegioconnect.listings;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/listings")
public class DiscoveryFeedController {
    private final DiscoveryFeedService feed;

    public DiscoveryFeedController(DiscoveryFeedService feed) {
        this.feed = feed;
    }

    @GetMapping("/feed")
    @Operation(summary = "Discover published opportunities",
            description = "Filters currently eligible, published, unexpired listings. "
                    + "deadlineOnOrBefore is an inclusive America/Puerto_Rico calendar date; "
                    + "listings without a closing time are not published. "
                    + "Results are ordered by publication time and ID; page starts at zero and size is 1 to 100. "
                    + "The old field filter returns 400 because field is not part of the approved model.")
    public List<FeedItem> find(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @Parameter(deprecated = true, description = "Temporary alias for category; remove after Luis migrates.")
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sourceLabel,
            @RequestParam(required = false) UUID organizationId,
            @Parameter(description = "Inclusive Puerto Rico local date (YYYY-MM-DD).")
            @RequestParam(required = false) String deadlineOnOrBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @RequestParam Map<String, String> allParameters) {
        if (allParameters.containsKey("field")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "field is no longer supported; use the approved feed filters.");
        }
        OpportunityType selected = OpportunityType.fromFilter(category);
        OpportunityType legacy = OpportunityType.fromFilter(type);
        if (selected != null && legacy != null && selected != legacy) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "category and type must resolve to the same category.");
        }
        LocalDate deadline = null;
        if (deadlineOnOrBefore != null) {
            try {
                deadline = LocalDate.parse(deadlineOnOrBefore);
            } catch (DateTimeParseException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "deadlineOnOrBefore must be an ISO date (YYYY-MM-DD).");
            }
        }
        return feed.find(keyword, selected == null ? legacy : selected,
                sourceLabel, organizationId, deadline, page, size);
    }
}

package com.colegioconnect.listings;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/listings")
public class DiscoveryFeedController {

    private final DiscoveryFeedService feed;

    public DiscoveryFeedController(DiscoveryFeedService feed) {
        this.feed = feed;
    }

    @GetMapping("/feed")
    @Operation(summary = "Discover published opportunities",
            description = "Returns a bounded array ordered by publication time and ID; "
                    + "page starts at zero and size is 1 to 100.")
    public List<FeedItem> find(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String field,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return feed.find(keyword, OpportunityType.fromFilter(type), field, page, size);
    }
}

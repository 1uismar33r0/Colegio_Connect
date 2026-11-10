package com.colegioconnect.listings;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"stats", "recent"})
public record OfficeOverviewResponse(OfficeStats stats, List<OfficeRecentItem> recent) {

    @Schema(requiredProperties = {"published", "newResponses", "drafts"})
    public record OfficeStats(long published, long newResponses, long drafts) {
    }

    @Schema(requiredProperties = {"id", "title", "type", "newResponses", "status"})
    public record OfficeRecentItem(UUID id, String title, String type,
                                   @Schema(nullable = true) Long newResponses,
                                   OfficeListingStatus status) {
    }

    public enum OfficeListingStatus {
        PUBLISHED,
        DRAFT,
        CLOSED
    }
}

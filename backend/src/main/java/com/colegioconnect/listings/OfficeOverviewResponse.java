package com.colegioconnect.listings;

import java.util.List;
import java.util.UUID;

public record OfficeOverviewResponse(OfficeStats stats, List<OfficeRecentItem> recent) {

    public record OfficeStats(long published, long newResponses, long drafts) {
    }

    public record OfficeRecentItem(UUID id, String title, String type,
                                   Long newResponses, OfficeListingStatus status) {
    }

    public enum OfficeListingStatus {
        PUBLISHED,
        DRAFT,
        CLOSED
    }
}

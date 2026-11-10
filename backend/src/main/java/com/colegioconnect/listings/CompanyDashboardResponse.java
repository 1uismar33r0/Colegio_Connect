package com.colegioconnect.listings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompanyDashboardResponse(CompanyStats stats, List<CompanyActiveItem> active,
                                       CompanyAttention attention) {

    public record CompanyStats(long activeListings, long responses,
                               CompanyProfileStatus profileStatus) {
    }

    public record CompanyActiveItem(UUID id, String title, String type,
                                    Instant closesAt, long responses) {
    }

    public record CompanyAttention(long newResponses) {
    }

    public enum CompanyProfileStatus {
        COMPLETE,
        INCOMPLETE,
        PENDING_APPROVAL
    }
}

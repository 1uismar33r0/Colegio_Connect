package com.colegioconnect.listings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"stats", "active", "attention"})
public record CompanyDashboardResponse(CompanyStats stats, List<CompanyActiveItem> active,
                                       CompanyAttention attention) {

    @Schema(requiredProperties = {"activeListings", "responses", "profileStatus"})
    public record CompanyStats(long activeListings, long responses,
                               CompanyProfileStatus profileStatus) {
    }

    @Schema(requiredProperties = {"id", "title", "type", "closesAt", "responses"})
    public record CompanyActiveItem(UUID id, String title, String type,
                                    Instant closesAt, long responses) {
    }

    @Schema(requiredProperties = {"newResponses"})
    public record CompanyAttention(long newResponses) {
    }

    public enum CompanyProfileStatus {
        COMPLETE,
        INCOMPLETE,
        PENDING_APPROVAL
    }
}

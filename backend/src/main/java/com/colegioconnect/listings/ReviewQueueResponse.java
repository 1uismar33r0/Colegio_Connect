package com.colegioconnect.listings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"stats", "items"})
public record ReviewQueueResponse(ReviewQueueStats stats, List<ReviewQueueItem> items) {

    @Schema(requiredProperties = {"awaitingReview", "reportedListings", "pendingVerification"})
    public record ReviewQueueStats(long awaitingReview, long reportedListings,
                                   long pendingVerification) {
    }

    @Schema(requiredProperties = {"id", "submission", "submittedBy", "reason", "receivedAt"})
    public record ReviewQueueItem(UUID id, String submission, String submittedBy,
                                  ReviewReason reason,
                                  @Schema(nullable = true, description = "Unknown for legacy "
                                          + "organization records without a UUIDv7 timestamp")
                                  Instant receivedAt) {
    }

    public enum ReviewReason {
        NEW_ORGANIZATION,
        REPORTED_LISTING,
        PROFILE_VERIFICATION
    }
}

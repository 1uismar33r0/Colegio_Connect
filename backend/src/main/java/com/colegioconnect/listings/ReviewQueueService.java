package com.colegioconnect.listings;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.colegioconnect.listings.ReviewQueueResponse.ReviewQueueItem;
import com.colegioconnect.listings.ReviewQueueResponse.ReviewQueueStats;
import com.colegioconnect.listings.ReviewQueueResponse.ReviewReason;

@Service
public class ReviewQueueService {

    private final NamedParameterJdbcTemplate jdbc;

    public ReviewQueueService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ReviewQueueResponse find(int page, int size) {
        if (page < 0 || page > 10_000 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be between 0 and 10000; size must be between 1 and 100");
        }
        long[] counts = jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM organizations
                         WHERE org_type = 'EXTERNAL_COMPANY'
                           AND approval_status = 'PENDING') AS companies,
                       (SELECT count(DISTINCT listing_id) FROM listing_reports
                         WHERE status = 'OPEN') AS reported_listings
                """, Map.of(), (rs, rowNumber) -> new long[] {
                        rs.getLong("companies"), rs.getLong("reported_listings")
                });
        List<ReviewQueueItem> items = jdbc.query("""
                SELECT id, submission, submitted_by, reason, received_at
                  FROM (
                    SELECT organization.id, organization.name AS submission,
                           organization.name AS submitted_by,
                           'NEW_ORGANIZATION' AS reason,
                           organization.review_received_at AS received_at
                      FROM organizations organization
                     WHERE organization.org_type = 'EXTERNAL_COMPANY'
                       AND organization.approval_status = 'PENDING'
                    UNION ALL
                    SELECT listing.id, listing.title AS submission,
                           organization.name AS submitted_by,
                           'REPORTED_LISTING' AS reason,
                           max(report.received_at) AS received_at
                      FROM listing_reports report
                      JOIN listings listing ON listing.id = report.listing_id
                      JOIN organizations organization ON organization.id = listing.organization_id
                     WHERE report.status = 'OPEN'
                     GROUP BY listing.id, listing.title, organization.name
                  ) queue
                 ORDER BY received_at DESC NULLS LAST, id DESC
                 LIMIT :size OFFSET :offset
                """, Map.of("size", size, "offset", (long) page * size),
                (rs, rowNumber) -> new ReviewQueueItem(
                        rs.getObject("id", UUID.class), rs.getString("submission"),
                        rs.getString("submitted_by"),
                        ReviewReason.valueOf(rs.getString("reason")),
                        rs.getObject("received_at", OffsetDateTime.class) == null ? null
                                : rs.getObject("received_at", OffsetDateTime.class).toInstant()));
        return new ReviewQueueResponse(
                new ReviewQueueStats(counts[0] + counts[1], counts[1], counts[0]), items);
    }
}

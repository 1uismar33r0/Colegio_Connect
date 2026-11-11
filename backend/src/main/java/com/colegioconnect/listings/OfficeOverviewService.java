package com.colegioconnect.listings;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colegioconnect.listings.OfficeOverviewResponse.OfficeListingStatus;
import com.colegioconnect.listings.OfficeOverviewResponse.OfficeRecentItem;
import com.colegioconnect.listings.OfficeOverviewResponse.OfficeStats;

@Service
public class OfficeOverviewService {

    private final DashboardAccess access;
    private final NamedParameterJdbcTemplate jdbc;

    public OfficeOverviewService(DashboardAccess access, NamedParameterJdbcTemplate jdbc) {
        this.access = access;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public OfficeOverviewResponse forOrganization(UUID actorId, UUID organizationId) {
        organizationId = access.resolveOfficeId(actorId, organizationId);
        access.requireOffice(actorId, organizationId);
        Map<String, ?> parameters = Map.of("organizationId", organizationId);
        OfficeStats stats = jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM listings
                         WHERE organization_id = :organizationId
                           AND status = 'PUBLISHED'
                           AND closes_at > CURRENT_TIMESTAMP) AS published,
                       (SELECT count(*) FROM listings
                         WHERE organization_id = :organizationId
                           AND status = 'DRAFT') AS drafts,
                       (SELECT count(*) FROM listing_responses response
                         JOIN listings listing ON listing.id = response.listing_id
                         WHERE listing.organization_id = :organizationId
                           AND response.status = 'ACTIVE'
                           AND response.seen_at IS NULL) AS new_responses
                """, parameters, (rs, rowNumber) -> new OfficeStats(
                rs.getLong("published"), rs.getLong("new_responses"), rs.getLong("drafts")));
        List<OfficeRecentItem> recent = jdbc.query("""
                WITH recent AS (
                    SELECT id, title, opportunity_type,
                           CASE WHEN status = 'PUBLISHED' AND closes_at <= CURRENT_TIMESTAMP
                                THEN 'CLOSED' ELSE status END AS status,
                           created_at
                      FROM listings
                     WHERE organization_id = :organizationId
                       AND status IN ('PUBLISHED', 'DRAFT', 'CLOSED')
                     ORDER BY created_at DESC, id DESC
                     LIMIT 10
                )
                SELECT recent.id, recent.title, recent.opportunity_type, recent.status,
                       CASE WHEN recent.status = 'DRAFT' THEN NULL
                            ELSE (SELECT count(*) FROM listing_responses response
                                   WHERE response.listing_id = recent.id
                                     AND response.status = 'ACTIVE'
                                     AND response.seen_at IS NULL)
                        END AS new_responses
                  FROM recent
                 ORDER BY recent.created_at DESC, recent.id DESC
                """, parameters, (rs, rowNumber) -> new OfficeRecentItem(
                rs.getObject("id", UUID.class), rs.getString("title"),
                OpportunityType.valueOf(rs.getString("opportunity_type")).label(),
                rs.getObject("new_responses") == null ? null : rs.getLong("new_responses"),
                OfficeListingStatus.valueOf(rs.getString("status"))));
        return new OfficeOverviewResponse(stats, recent);
    }
}

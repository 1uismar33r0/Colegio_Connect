package com.colegioconnect.listings;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.listings.CompanyDashboardResponse.CompanyActiveItem;
import com.colegioconnect.listings.CompanyDashboardResponse.CompanyAttention;
import com.colegioconnect.listings.CompanyDashboardResponse.CompanyProfileStatus;
import com.colegioconnect.listings.CompanyDashboardResponse.CompanyStats;

@Service
public class CompanyDashboardService {

    private final DashboardAccess access;
    private final NamedParameterJdbcTemplate jdbc;

    public CompanyDashboardService(DashboardAccess access, NamedParameterJdbcTemplate jdbc) {
        this.access = access;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public CompanyDashboardResponse forOrganization(UUID actorId, UUID organizationId) {
        Organization organization = access.companyForDashboard(actorId, organizationId);
        organizationId = organization.getId();
        if (organization.getApprovalStatus() == ApprovalStatus.PENDING) {
            return new CompanyDashboardResponse(
                    new CompanyStats(0, 0, CompanyProfileStatus.PENDING_APPROVAL),
                    List.of(), new CompanyAttention(0));
        }
        Map<String, ?> parameters = Map.of("organizationId", organizationId);
        CompanyStats stats = jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM listings
                         WHERE organization_id = :organizationId
                           AND status = 'PUBLISHED'
                           AND closes_at > CURRENT_TIMESTAMP) AS active_listings,
                       (SELECT count(*) FROM listing_responses response
                         JOIN listings listing ON listing.id = response.listing_id
                         WHERE listing.organization_id = :organizationId
                           AND response.status = 'ACTIVE') AS responses
                """, parameters, (rs, rowNumber) -> new CompanyStats(
                rs.getLong("active_listings"), rs.getLong("responses"),
                CompanyProfileStatus.INCOMPLETE));
        CompanyAttention attention = jdbc.queryForObject("""
                SELECT count(*) AS new_responses
                  FROM listing_responses response
                  JOIN listings listing ON listing.id = response.listing_id
                 WHERE listing.organization_id = :organizationId
                   AND response.status = 'ACTIVE'
                   AND response.seen_at IS NULL
                """, parameters, (rs, rowNumber) -> new CompanyAttention(
                rs.getLong("new_responses")));
        List<CompanyActiveItem> active = jdbc.query("""
                WITH active AS (
                    SELECT id, title, opportunity_type, closes_at
                      FROM listings
                     WHERE organization_id = :organizationId
                       AND status = 'PUBLISHED'
                       AND closes_at > CURRENT_TIMESTAMP
                     ORDER BY closes_at ASC, id ASC
                     LIMIT 10
                )
                SELECT active.id, active.title, active.opportunity_type, active.closes_at,
                       (SELECT count(*) FROM listing_responses response
                         WHERE response.listing_id = active.id
                           AND response.status = 'ACTIVE') AS responses
                  FROM active
                 ORDER BY active.closes_at ASC, active.id ASC
                """, parameters, (rs, rowNumber) -> new CompanyActiveItem(
                rs.getObject("id", UUID.class), rs.getString("title"),
                OpportunityType.valueOf(rs.getString("opportunity_type")).label(),
                rs.getObject("closes_at", OffsetDateTime.class).toInstant(),
                rs.getLong("responses")));
        return new CompanyDashboardResponse(stats, active, attention);
    }
}

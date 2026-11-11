package com.colegioconnect.listings;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiscoveryFeedService {
    private static final ZoneId PUERTO_RICO = ZoneId.of("America/Puerto_Rico");
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public DiscoveryFeedService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FeedItem> find(String keyword, OpportunityType category, String sourceLabel,
                               UUID organizationId, LocalDate deadlineOnOrBefore,
                               int page, int size) {
        if (page < 0 || page > 10_000 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be between 0 and 10000; size must be between 1 and 100");
        }
        String normalizedKeyword = keyword == null ? null : keyword.strip();
        if (normalizedKeyword != null && (normalizedKeyword.isEmpty()
                || normalizedKeyword.length() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "keyword must be between 1 and 100 characters");
        }
        TrustLabel label = TrustLabel.fromFilter(sourceLabel);
        Instant now = clock.instant();
        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.title, COALESCE(o.name, team.name) AS source, l.trust_label,
                       l.posted_at, l.opportunity_type, l.summary, l.tags
                  FROM listings l
                  LEFT JOIN organizations o ON o.id = l.organization_id
                  LEFT JOIN capstone_teams team ON team.id = l.capstone_team_id
                  JOIN users publisher ON publisher.id = l.publisher_user_id
                 WHERE l.status = 'PUBLISHED'
                   AND l.closes_at > :now
                   AND publisher.status = 'ACTIVE' AND publisher.email_verified = TRUE
                   AND ((l.trust_label = 'VERIFIED_UPRM_INSTITUTION'
                         AND publisher.account_type = 'UPRM_MEMBER'
                         AND o.org_type IN ('UPRM_OFFICE', 'UPRM_DEPARTMENT')
                         AND EXISTS (SELECT 1 FROM role_assignments ra
                                      WHERE ra.user_id = publisher.id
                                        AND ra.organization_id = o.id
                                        AND ra.role = 'INSTITUTIONAL'))
                     OR (l.trust_label = 'APPROVED_EXTERNAL_COMPANY'
                         AND publisher.account_type = 'EXTERNAL_ORGANIZATION'
                         AND o.org_type = 'EXTERNAL_COMPANY'
                         AND o.approval_status = 'APPROVED'
                         AND EXISTS (SELECT 1 FROM role_assignments ra
                                      WHERE ra.user_id = publisher.id
                                        AND ra.organization_id = o.id
                                        AND ra.role = 'COMPANY'))
                     OR (l.trust_label = 'VERIFIED_STUDENT_PROJECT'
                         AND l.opportunity_type = 'CAPSTONE'
                         AND team.owner_user_id = publisher.id
                         AND publisher.account_type = 'UPRM_MEMBER'
                         AND EXISTS (SELECT 1 FROM role_assignments ra
                                      WHERE ra.user_id = publisher.id
                                        AND ra.role = 'STUDENT')))
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("now", java.sql.Timestamp.from(now))
                .addValue("size", size).addValue("offset", (long) page * size);
        if (category != null) {
            sql.append(" AND l.opportunity_type = :category");
            parameters.addValue("category", category.name());
        }
        if (label != null) {
            sql.append(" AND l.trust_label = :sourceLabel");
            parameters.addValue("sourceLabel", label.name());
        }
        if (organizationId != null) {
            sql.append(" AND l.organization_id = :organizationId");
            parameters.addValue("organizationId", organizationId);
        }
        if (deadlineOnOrBefore != null) {
            Instant exclusiveEnd = deadlineOnOrBefore.plusDays(1)
                    .atStartOfDay(PUERTO_RICO).toInstant();
            sql.append(" AND l.closes_at < :deadlineExclusiveEnd");
            parameters.addValue("deadlineExclusiveEnd", java.sql.Timestamp.from(exclusiveEnd));
        }
        if (normalizedKeyword != null) {
            sql.append(" AND to_tsvector('simple', l.title || ' ' || l.summary) "
                    + "@@ websearch_to_tsquery('simple', :keyword)");
            parameters.addValue("keyword", normalizedKeyword);
        }
        sql.append(" ORDER BY l.posted_at DESC, l.id DESC LIMIT :size OFFSET :offset");
        return jdbc.query(sql.toString(), parameters,
                (rs, rowNumber) -> new FeedItem(
                        rs.getObject("id", UUID.class), rs.getString("title"),
                        rs.getString("source"), TrustLabel.valueOf(rs.getString("trust_label")).label(),
                        rs.getObject("posted_at", java.time.OffsetDateTime.class).toInstant(),
                        OpportunityType.valueOf(rs.getString("opportunity_type")).label(),
                        rs.getString("summary"), tags(rs.getArray("tags"))));
    }

    private List<String> tags(Array array) throws SQLException {
        return Arrays.asList((String[]) array.getArray());
    }
}

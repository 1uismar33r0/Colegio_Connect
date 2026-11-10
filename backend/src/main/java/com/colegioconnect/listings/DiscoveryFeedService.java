package com.colegioconnect.listings;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiscoveryFeedService {

    private final NamedParameterJdbcTemplate jdbc;

    public DiscoveryFeedService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<FeedItem> find(String keyword, OpportunityType type, String field,
                               int page, int size) {
        if (page < 0 || page > 10_000 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be between 0 and 10000; size must be between 1 and 100");
        }
        String normalizedKeyword = keyword == null ? null : keyword.strip();
        String normalizedField = field == null ? null : field.strip().toLowerCase(Locale.ROOT);
        if (normalizedKeyword != null && (normalizedKeyword.isEmpty()
                || normalizedKeyword.length() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "keyword must be between 1 and 100 characters");
        }
        if (normalizedField != null && (normalizedField.isEmpty()
                || normalizedField.length() > 120)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "field must be between 1 and 120 characters");
        }

        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.title, o.name AS source, l.posted_at, l.opportunity_type,
                       l.summary, l.tags
                  FROM listings l
                  JOIN organizations o ON o.id = l.organization_id
                  JOIN users publisher ON publisher.id = l.publisher_user_id
                 WHERE l.status = 'PUBLISHED'
                   AND l.visibility = 'PUBLIC'
                   AND l.closes_at > CURRENT_TIMESTAMP
                   AND publisher.status = 'ACTIVE' AND publisher.email_verified = TRUE
                   AND ((o.org_type IN ('UPRM_OFFICE', 'UPRM_DEPARTMENT')
                         AND EXISTS (SELECT 1 FROM role_assignments ra
                                      WHERE ra.user_id = publisher.id
                                        AND ra.organization_id = o.id
                                        AND ra.role = 'INSTITUTIONAL'))
                     OR (o.org_type = 'EXTERNAL_COMPANY'
                         AND o.approval_status = 'APPROVED'
                         AND EXISTS (SELECT 1 FROM role_assignments ra
                                      WHERE ra.user_id = publisher.id
                                        AND ra.organization_id = o.id
                                        AND ra.role = 'COMPANY')))
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("size", size).addValue("offset", (long) page * size);
        if (type != null) {
            sql.append(" AND l.opportunity_type = :type");
            parameters.addValue("type", type.name());
        }
        if (normalizedField != null) {
            sql.append(" AND lower(l.field) = :field");
            parameters.addValue("field", normalizedField);
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
                        rs.getString("source"), rs.getObject("posted_at", java.time.OffsetDateTime.class)
                                .toInstant(), OpportunityType.valueOf(rs.getString("opportunity_type")).label(),
                        rs.getString("summary"), tags(rs.getArray("tags"))));
    }

    private List<String> tags(Array array) throws SQLException {
        return Arrays.asList((String[]) array.getArray());
    }
}

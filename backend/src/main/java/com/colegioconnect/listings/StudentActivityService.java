package com.colegioconnect.listings;

import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentActivityService {

    private final NamedParameterJdbcTemplate jdbc;

    public StudentActivityService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public StudentActivityResponse forUser(UUID userId) {
        return jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM listing_responses
                         WHERE user_id = :userId AND status = 'ACTIVE') AS active_responses,
                       (SELECT count(*) FROM saved_listings
                         WHERE user_id = :userId) AS saved
                """, Map.of("userId", userId), (rs, rowNumber) -> new StudentActivityResponse(
                rs.getLong("active_responses"), rs.getLong("saved")));
    }
}

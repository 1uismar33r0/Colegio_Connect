package com.colegioconnect.listings;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum OpportunityType {
    JOB,
    INTERNSHIP,
    COOP,
    RESEARCH_POSITION,
    RESEARCH_PARTICIPATION,
    CAPSTONE;

    public String label() {
        return switch (this) {
            case JOB -> "Job";
            case INTERNSHIP -> "Internship";
            case COOP -> "Co-op";
            case RESEARCH_POSITION -> "Research position";
            case RESEARCH_PARTICIPATION -> "Research participation";
            case CAPSTONE -> "Capstone sponsorship";
        };
    }

    public static OpportunityType fromFilter(String value) {
        if (value == null) return null;
        String normalized = value.strip().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        if (normalized.equals("CAPSTONE_SPONSORSHIP")) normalized = "CAPSTONE";
        try {
            return OpportunityType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported opportunity type.");
        }
    }
}

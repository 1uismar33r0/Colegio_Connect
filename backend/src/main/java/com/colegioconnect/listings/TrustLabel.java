package com.colegioconnect.listings;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum TrustLabel {
    VERIFIED_UPRM_INSTITUTION("Verified UPRM institution"),
    APPROVED_EXTERNAL_COMPANY("Approved external company"),
    VERIFIED_STUDENT_PROJECT("Verified student project");

    private final String label;
    TrustLabel(String label) { this.label = label; }
    public String label() { return label; }

    public static TrustLabel fromFilter(String value) {
        if (value == null) return null;
        String normalized = value.strip().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        try {
            return TrustLabel.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sourceLabel.");
        }
    }
}

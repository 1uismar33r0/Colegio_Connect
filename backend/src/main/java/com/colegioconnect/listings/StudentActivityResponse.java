package com.colegioconnect.listings;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"activeResponses", "saved"})
public record StudentActivityResponse(long activeResponses, long saved) {
}

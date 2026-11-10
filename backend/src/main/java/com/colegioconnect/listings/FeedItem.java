package com.colegioconnect.listings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"id", "title", "source", "postedAt", "type", "summary", "tags"})
public record FeedItem(UUID id, String title, String source, Instant postedAt,
                       String type, String summary, List<String> tags) {
}

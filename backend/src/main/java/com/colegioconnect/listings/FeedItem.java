package com.colegioconnect.listings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedItem(UUID id, String title, String source, Instant postedAt,
                       String type, String summary, List<String> tags) {
}

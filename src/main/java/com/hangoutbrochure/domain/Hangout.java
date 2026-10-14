package com.hangoutbrochure.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Hangout(
        UUID id,
        String title,
        List<String> categories,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String locationName,
        String address,
        double latitude,
        double longitude
) {
}

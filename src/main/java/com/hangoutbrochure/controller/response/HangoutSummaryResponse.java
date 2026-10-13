package com.hangoutbrochure.controller.response;

import java.time.OffsetDateTime;

public record HangoutSummaryResponse(
        String id,
        String title,
        String categoryId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String locationName,
        String address,
        double latitude,
        double longitude
) {
}

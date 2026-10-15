package com.hangoutbrochure.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record HangoutSummaryResponse(
        String id,
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

package com.hangoutbrochure.command.ingestion.source.seoulCulturalEvent;

import java.math.BigDecimal;

/** A normalized representation of one row from Seoul's cultural event API. */
public record SeoulCulturalEventRecord(
        String category,
        String district,
        String title,
        String period,
        String place,
        String organization,
        String audience,
        String fee,
        String inquiry,
        String performers,
        String program,
        String description,
        String homepageUrl,
        String imageUrl,
        String applicationDate,
        String ticketType,
        String startsOn,
        String endsOn,
        String theme,
        BigDecimal longitude,
        BigDecimal latitude,
        String freeOrPaid,
        String sourceUrl,
        String eventTime,
        String rawPayload
) {
}

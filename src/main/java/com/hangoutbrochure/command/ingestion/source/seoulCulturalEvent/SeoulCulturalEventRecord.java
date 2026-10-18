package com.hangoutbrochure.command.ingestion.source.seoulCulturalEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row of Seoul's cultural event API, mapped onto the names this project uses. */
public record SeoulCulturalEventRecord(
        long cultcode,
        String detailUrl,
        String title,
        String sourceCategory,
        String sourceTheme,
        String district,
        String place,
        String organizer,
        String organizerUrl,
        LocalDate startDate,
        LocalDate endDate,
        String eventTimeDesc,
        LocalDate registerDate,
        String targetAudience,
        String feeDesc,
        Boolean isFree,
        String ticketType,
        String contact,
        String performers,
        String programDesc,
        String etcDesc,
        String imageUrl,
        BigDecimal latitude,
        BigDecimal longitude
) {
}

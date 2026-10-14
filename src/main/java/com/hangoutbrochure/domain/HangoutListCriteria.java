package com.hangoutbrochure.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record HangoutListCriteria(
        List<String> categories,
        OffsetDateTime from,
        OffsetDateTime to,
        UUID cursorId,
        int limit
) {
    public HangoutListCriteria {
        categories = List.copyOf(categories);
        if (from == null || to == null) {
            throw new IllegalArgumentException("From and to date-times are required");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be positive");
        }
    }
}

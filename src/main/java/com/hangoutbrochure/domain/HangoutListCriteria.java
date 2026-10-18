package com.hangoutbrochure.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HangoutListCriteria(
        List<String> categories,
        LocalDate from,
        LocalDate to,
        UUID cursorId,
        int limit
) {
    public HangoutListCriteria {
        categories = List.copyOf(categories);
        if (from == null || to == null) {
            throw new IllegalArgumentException("From and to dates are required");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be positive");
        }
    }
}

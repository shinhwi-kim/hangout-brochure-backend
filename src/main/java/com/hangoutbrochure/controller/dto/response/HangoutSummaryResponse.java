package com.hangoutbrochure.controller.dto.response;

import java.time.LocalDate;
import java.util.List;

public record HangoutSummaryResponse(
        String id,
        String title,
        List<String> categories,
        LocalDate startDate,
        LocalDate endDate,
        String eventTimeDesc,
        String locationName,
        String address,
        String district,
        double latitude,
        double longitude,
        String organizer,
        String imageUrl,
        String detailUrl,
        Boolean isFree,
        String feeDesc,
        String contact
) {
}

package com.hangoutbrochure.controller.response;

import java.util.List;

public record HangoutListResponse(
        List<HangoutSummaryResponse> items,
        int size,
        String nextCursor,
        boolean hasNext
) {
}

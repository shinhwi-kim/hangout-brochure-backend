package com.hangoutbrochure.controller.dto.response;

import java.util.List;

public record HangoutCategoryListResponse(
        List<HangoutCategoryResponse> items,
        int size,
        Integer nextCursor,
        boolean hasNext
) {
    public HangoutCategoryListResponse {
        items = List.copyOf(items);
    }
}

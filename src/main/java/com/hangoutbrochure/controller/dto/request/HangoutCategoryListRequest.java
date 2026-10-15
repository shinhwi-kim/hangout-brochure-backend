package com.hangoutbrochure.controller.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record HangoutCategoryListRequest(
        String keyword,

        @Min(1)
        Integer cursor,

        @Min(1) @Max(500)
        Integer size
) {
    public HangoutCategoryListRequest {
        size = size == null ? 100 : size;
    }
}

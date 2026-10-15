package com.hangoutbrochure.service;

import com.hangoutbrochure.domain.HangoutCategory;

import java.util.List;

public record HangoutCategoryListResult(
        List<HangoutCategory> items,
        Integer nextCursor,
        boolean hasNext
) {
    public HangoutCategoryListResult {
        items = List.copyOf(items);
    }
}

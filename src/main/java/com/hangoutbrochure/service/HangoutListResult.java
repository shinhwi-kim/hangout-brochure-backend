package com.hangoutbrochure.service;

import com.hangoutbrochure.domain.Hangout;

import java.util.List;

public record HangoutListResult(
        List<Hangout> items,
        String nextCursor,
        boolean hasNext
) {
    public HangoutListResult {
        items = List.copyOf(items);
    }
}

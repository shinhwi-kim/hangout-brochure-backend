package com.hangoutbrochure.domain;

public record HangoutCategoryListCriteria(
        String keyword,
        Integer cursor,
        int pageSize
) {
    public HangoutCategoryListCriteria {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be positive");
        }
    }
}

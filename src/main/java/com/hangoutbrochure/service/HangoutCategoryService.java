package com.hangoutbrochure.service;

import com.hangoutbrochure.domain.HangoutCategory;
import com.hangoutbrochure.domain.HangoutCategoryListCriteria;
import com.hangoutbrochure.repository.HangoutRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HangoutCategoryService {

    private final HangoutRepository hangoutRepository;

    public HangoutCategoryService(HangoutRepository hangoutRepository) {
        this.hangoutRepository = hangoutRepository;
    }

    public HangoutCategoryListResult getHangoutCategories(String keyword, Integer cursor, int size) {
        HangoutCategoryListCriteria criteria = new HangoutCategoryListCriteria(
                keyword,
                cursor,
                size + 1
        );

        List<HangoutCategory> items = new ArrayList<>(
                hangoutRepository.findHangoutCategories(criteria)
        );
        boolean hasNext = items.size() > size;
        if (hasNext) {
            items.remove(items.size() - 1);
        }

        Integer nextCursor = hasNext ? items.get(items.size() - 1).id() : null;
        return new HangoutCategoryListResult(items, nextCursor, hasNext);
    }
}

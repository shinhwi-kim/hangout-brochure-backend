package com.hangoutbrochure.service;

import com.hangoutbrochure.domain.Hangout;
import com.hangoutbrochure.domain.HangoutListCriteria;
import com.hangoutbrochure.repository.HangoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class HangoutService {

    private final HangoutRepository hangoutRepository;

    public HangoutService(HangoutRepository hangoutRepository) {
        this.hangoutRepository = hangoutRepository;
    }

    @Transactional(readOnly = true)
    public HangoutListResult getHangouts(
            List<String> categories,
            LocalDate from,
            LocalDate to,
            int size,
            UUID cursorId
    ) {
        List<String> normalizedCategories = normalizeCategories(categories);

        HangoutListCriteria criteria = new HangoutListCriteria(
                normalizedCategories,
                from,
                to,
                cursorId,
                size + 1
        );

        List<Hangout> page = new ArrayList<>(hangoutRepository.findHangouts(criteria));
        boolean hasNext = page.size() > size;
        if (hasNext) {
            page.remove(page.size() - 1);
        }

        String nextCursor = hasNext ? page.get(page.size() - 1).id().toString() : null;
        return new HangoutListResult(page, nextCursor, hasNext);
    }

    private List<String> normalizeCategories(List<String> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
                .filter(category -> category != null && !category.isBlank())
                .map(String::trim)
                .map(category -> category.toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
    }
}

package com.hangoutbrochure.controller;

import com.hangoutbrochure.controller.dto.request.HangoutListRequest;
import com.hangoutbrochure.controller.dto.request.HangoutCategoryListRequest;
import com.hangoutbrochure.controller.dto.response.HangoutCategoryListResponse;
import com.hangoutbrochure.controller.dto.response.HangoutCategoryResponse;
import com.hangoutbrochure.controller.dto.response.HangoutListResponse;
import com.hangoutbrochure.controller.dto.response.HangoutSummaryResponse;
import com.hangoutbrochure.domain.Hangout;
import com.hangoutbrochure.domain.HangoutCategory;
import com.hangoutbrochure.service.HangoutCategoryListResult;
import com.hangoutbrochure.service.HangoutCategoryService;
import com.hangoutbrochure.service.HangoutListResult;
import com.hangoutbrochure.service.HangoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HangoutBrochureController {

    private final HangoutService hangoutService;
    private final HangoutCategoryService hangoutCategoryService;

    public HangoutBrochureController(
            HangoutService hangoutService,
            HangoutCategoryService hangoutCategoryService
    ) {
        this.hangoutService = hangoutService;
        this.hangoutCategoryService = hangoutCategoryService;
    }

    @GetMapping("/api/hello")
    public String hello() {
        return "HangoutBrochure server is running";
    }

    @GetMapping("/api/hangouts")
    public HangoutListResponse getHangouts(@Valid @ModelAttribute HangoutListRequest request) {
        HangoutListResult result = hangoutService.getHangouts(
                request.categories(),
                request.from(),
                request.to(),
                request.size(),
                request.cursor()
        );

        return new HangoutListResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.items().size(),
                result.nextCursor(),
                result.hasNext()
        );
    }

    @GetMapping("/api/hangout-categories")
    public HangoutCategoryListResponse getHangoutCategories(
            @Valid @ModelAttribute HangoutCategoryListRequest request
    ) {
        HangoutCategoryListResult result = hangoutCategoryService.getHangoutCategories(
                request.keyword(),
                request.cursor(),
                request.size()
        );
        return new HangoutCategoryListResponse(
                result.items().stream().map(this::toCategoryResponse).toList(),
                result.items().size(),
                result.nextCursor(),
                result.hasNext()
        );
    }

    private HangoutSummaryResponse toResponse(Hangout hangout) {
        return new HangoutSummaryResponse(
                hangout.id().toString(),
                hangout.title(),
                hangout.categories(),
                hangout.startsAt(),
                hangout.endsAt(),
                hangout.locationName(),
                hangout.address(),
                hangout.latitude(),
                hangout.longitude()
        );
    }

    private HangoutCategoryResponse toCategoryResponse(HangoutCategory category) {
        return new HangoutCategoryResponse(category.id(), category.code(), category.name());
    }
}

package com.hangoutbrochure.controller;

import com.hangoutbrochure.controller.request.HangoutListRequest;
import com.hangoutbrochure.controller.response.HangoutListResponse;
import com.hangoutbrochure.controller.response.HangoutSummaryResponse;
import com.hangoutbrochure.domain.Hangout;
import com.hangoutbrochure.service.HangoutListResult;
import com.hangoutbrochure.service.HangoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HangoutBrochureController {

    private final HangoutService hangoutService;

    public HangoutBrochureController(HangoutService hangoutService) {
        this.hangoutService = hangoutService;
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
}

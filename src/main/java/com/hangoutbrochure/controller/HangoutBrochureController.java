package com.hangoutbrochure.controller;

import com.hangoutbrochure.controller.request.HangoutSearchRequest;
import com.hangoutbrochure.controller.response.HangoutListResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HangoutBrochureController {

    @GetMapping("/api/hello")
    public String hello() {
        return "HangoutBrochure server is running";
    }

    @GetMapping("/api/hangouts")
    public HangoutListResponse getHangouts(@Valid @ModelAttribute HangoutSearchRequest request) {
        // The database-backed cursor lookup will be added with the persistence layer.
        return new HangoutListResponse(List.of(), request.getSize(), null, false);
    }
}

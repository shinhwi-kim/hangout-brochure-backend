package com.hangoutbrochure.controller.dto.request;

import com.hangoutbrochure.controller.dto.request.validation.ValidHangoutListRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ValidHangoutListRequest
public record HangoutListRequest(
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,

        List<String> categories,
        UUID cursor,

        @Min(1) @Max(500)
        Integer size
) {
    public HangoutListRequest {
        size = size == null ? 300 : size;
    }
}

package com.hangoutbrochure.controller.dto.response.error;

public record ApiFieldError(
        String field,
        String reason
) {
}

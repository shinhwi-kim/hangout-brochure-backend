package com.hangoutbrochure.controller.response;

public record ApiFieldError(
        String field,
        String reason
) {
}

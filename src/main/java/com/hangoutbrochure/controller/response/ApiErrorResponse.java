package com.hangoutbrochure.controller.response;

import java.util.List;

public record ApiErrorResponse(
        int status,
        String code,
        String message,
        List<ApiFieldError> errors,
        String path
) {
}

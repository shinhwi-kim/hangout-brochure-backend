package com.hangoutbrochure.controller.dto.response.error;

import java.util.List;

public record ApiErrorResponse(
        int status,
        String code,
        String message,
        List<ApiFieldError> errors,
        String path
) {
}

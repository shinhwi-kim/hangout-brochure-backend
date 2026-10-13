package com.hangoutbrochure.controller.advice;

import com.hangoutbrochure.controller.response.ApiErrorResponse;
import com.hangoutbrochure.controller.response.ApiFieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiFieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toApiFieldError)
                .toList();

        ApiErrorResponse body = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_PARAMETER",
                "Request parameters are invalid.",
                errors,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    private ApiFieldError toApiFieldError(FieldError error) {
        if ("typeMismatch".equals(error.getCode())) {
            String reason = "from".equals(error.getField()) || "to".equals(error.getField())
                    ? "must be a valid ISO 8601 date-time value"
                    : "has an invalid format or value";
            return new ApiFieldError(error.getField(), reason);
        }

        String reason = error.getDefaultMessage() == null
                ? "Invalid value."
                : error.getDefaultMessage();
        return new ApiFieldError(error.getField(), reason);
    }
}

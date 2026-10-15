package com.hangoutbrochure.controller.dto.request.validation;

import com.hangoutbrochure.controller.dto.request.HangoutListRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HangoutListRequestValidator implements ConstraintValidator<ValidHangoutListRequest, HangoutListRequest> {
    @Override
    public boolean isValid(HangoutListRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (request.from() != null && request.to() != null
                && request.from().isAfter(request.to())) {
            addViolation(context, "from must be before or equal to to", "from");
            valid = false;
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String message, String property) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}

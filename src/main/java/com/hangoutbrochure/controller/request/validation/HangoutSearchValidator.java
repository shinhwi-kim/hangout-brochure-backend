package com.hangoutbrochure.controller.request.validation;

import com.hangoutbrochure.controller.request.HangoutSearchRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HangoutSearchValidator implements ConstraintValidator<ValidHangoutSearch, HangoutSearchRequest> {
    @Override
    public boolean isValid(HangoutSearchRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (request.getFrom() != null && request.getTo() != null
                && request.getFrom().isAfter(request.getTo())) {
            addViolation(context, "from must be before or equal to to", "from");
            valid = false;
        }

        boolean anyBounds = request.getWest() != null || request.getSouth() != null
                || request.getEast() != null || request.getNorth() != null;
        boolean allBounds = request.getWest() != null && request.getSouth() != null
                && request.getEast() != null && request.getNorth() != null;
        if (anyBounds && !allBounds) {
            addViolation(context, "west, south, east, and north must be provided together", "west");
            valid = false;
        } else if (allBounds && request.getSouth() > request.getNorth()) {
            addViolation(context, "south must be less than or equal to north", "south");
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

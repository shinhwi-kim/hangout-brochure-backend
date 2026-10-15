package com.hangoutbrochure.controller.dto.request.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HangoutListRequestValidator.class)
public @interface ValidHangoutListRequest {
    String message() default "Invalid hangout search parameters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

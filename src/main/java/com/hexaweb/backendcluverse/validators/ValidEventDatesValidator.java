package com.hexaweb.backendcluverse.validators;

import com.hexaweb.backendcluverse.dto.EventRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidEventDatesValidator implements ConstraintValidator<ValidEventDates, EventRequest> {

    @Override
    public void initialize(ValidEventDates constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(EventRequest request, ConstraintValidatorContext context) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            return true; // Let @NotNull handle null validation
        }

        return request.getEndDate().isAfter(request.getStartDate());
    }
}
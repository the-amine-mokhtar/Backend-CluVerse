package com.hexaweb.backendcluverse.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class NoProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    private static final List<String> BAD_WORDS = Arrays.asList(
        "merde", "putain", "connard", "salope", "enculé", "bite", "cul", "foutre", "bordel",
        "fuck", "shit", "asshole", "bitch", "damn", "crap", "bastard", "dick", "pussy"
    );

    @Override
    public void initialize(NoProfanity constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotBlank handle null/empty validation
        }

        String lowerValue = value.toLowerCase();
        return BAD_WORDS.stream().noneMatch(lowerValue::contains);
    }
}
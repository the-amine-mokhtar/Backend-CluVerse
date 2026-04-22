package com.hexaweb.backendcluverse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import com.hexaweb.backendcluverse.enumerations.EventCategory;
import com.hexaweb.backendcluverse.validators.NoProfanity;
import com.hexaweb.backendcluverse.validators.ValidEventDates;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ValidEventDates
public class EventRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    @NoProfanity
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @NoProfanity
    private String description;

    @NotBlank(message = "Location is required")
    @Size(min = 3, max = 200, message = "Location must be between 3 and 200 characters")
    @NoProfanity
    private String location;

    private String latitude;
    private String longitude;

    private String imageUrl;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private Long clubId;
    private Long campaignId; // optionnel

    // ✅ ENUM propre
    private EventStatus status;

    private String category;

    // 💳 Paiement
    @NotNull(message = "isPaid must be specified")
    private Boolean isPaid;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private Double price;

    // ✅ Validation personnalisée : prix obligatoire si isPaid = true
    @AssertTrue(message = "Price must be specified and greater than 0 when event is paid")
    public boolean isPriceValidForPaidEvents() {
        if (isPaid != null && isPaid) {
            return price != null && price > 0;
        }
        return true; // Si gratuit, prix peut être null
    }

    private Boolean reminderSent = false;
}
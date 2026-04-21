package com.hexaweb.backendcluverse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import com.hexaweb.backendcluverse.enumerations.EventCategory;
import com.hexaweb.backendcluverse.validators.NoProfanity;
import com.hexaweb.backendcluverse.validators.ValidEventDates;
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
    @NoProfanity
    private String location;

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

    // ✅ NOUVEAU : Category
    private EventCategory category;

    // 💳 Paiement
    @NotNull(message = "isPaid must be specified")
    private Boolean isPaid;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private Double price;

    // Coordonnées GPS
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    // ✅ Validation personnalisée : prix obligatoire si isPaid = true
    @AssertTrue(message = "Price must be specified and greater than 0 when event is paid")
    public boolean isPriceValidForPaidEvents() {
        if (isPaid != null && isPaid) {
            return price != null && price > 0;
        }
        return true; // Si gratuit, prix peut être null
    }
}
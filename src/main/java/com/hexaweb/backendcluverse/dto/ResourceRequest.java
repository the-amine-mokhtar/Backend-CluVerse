package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.ResourceStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ResourceRequest {
    private String name;
    private String description;
    private double unitCost;
    private ResourceStatus status;
    private String imageUrl;
    private int quantityTotal;
    private int availableQuantity;
    private LocalDateTime lastUpdated;
    private int lowStockThreshold;
    private String notes;
    private Long clubId;
}

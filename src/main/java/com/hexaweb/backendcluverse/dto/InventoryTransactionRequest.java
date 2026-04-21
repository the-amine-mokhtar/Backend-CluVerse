package com.hexaweb.backendcluverse.dto;

import com.hexaweb.backendcluverse.enumerations.InventoryTransactionType;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryTransactionRequest {
    private InventoryTransactionType type;
    private int quantity;
    private LocalDateTime date;
    private String reason;
    private Long resourceId;
}

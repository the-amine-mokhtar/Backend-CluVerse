package com.hexaweb.backendcluverse.enumerations;

public enum CampaignStatus {
    PLANNED,
    ACTIVE,
    LOCKED,      // Bloqué - contient des événements avec participants
    DISABLED,    // Désactivé par admin
    ARCHIVED,   // Archivé - lecture seule
    FINISHED,
    CANCELLED
}

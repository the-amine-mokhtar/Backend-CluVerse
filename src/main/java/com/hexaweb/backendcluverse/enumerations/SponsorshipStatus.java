package com.hexaweb.backendcluverse.enumerations;

public enum SponsorshipStatus {
    PROSPECTING,
    OUTREACH_SENT,
    CONTRACT_SENT,
    SIGNED,
    PAID,

    // Legacy values kept for backwards compatibility with existing DB rows.
    PROPOSAL_SENT,
    PROPOSED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}


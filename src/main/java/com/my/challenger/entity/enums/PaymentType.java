package com.my.challenger.entity.enums;

/**
 * Enum representing payment types for challenges
 */
public enum PaymentType {
    FREE,           // No payment required
    ENTRY_FEE,      // Fixed entry fee
    PRIZE_POOL,     // Entry fees contribute to prize pool
    SUBSCRIPTION    // Requires active subscription
}
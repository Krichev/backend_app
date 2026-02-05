package com.my.challenger.entity.enums;

public enum MatchmakingStatus {
    QUEUED,    // In queue waiting for match
    MATCHED,   // Found opponent
    EXPIRED,   // Queue entry expired
    CANCELLED  // User cancelled queue
}

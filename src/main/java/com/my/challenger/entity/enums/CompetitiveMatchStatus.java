package com.my.challenger.entity.enums;

public enum CompetitiveMatchStatus {
    WAITING_FOR_OPPONENT,  // Match created, waiting for player2
    READY,                 // Both players joined, ready to start
    IN_PROGRESS,          // Match actively being played
    ROUND_COMPLETE,       // Current round finished, preparing next
    COMPLETED,            // All rounds finished
    CANCELLED,            // Match cancelled by player
    EXPIRED;              // Match expired due to timeout

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == EXPIRED;
    }

    public boolean canAcceptPlayers() {
        return this == WAITING_FOR_OPPONENT;
    }
}

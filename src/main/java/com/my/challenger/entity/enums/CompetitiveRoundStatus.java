package com.my.challenger.entity.enums;

public enum CompetitiveRoundStatus {
    PENDING,              // Round not started
    PLAYER1_PERFORMING,   // Player 1 is recording
    PLAYER2_PERFORMING,   // Player 2 is recording
    SCORING,              // Both submitted, calculating scores
    COMPLETED;            // Round scored and complete

    public boolean isActive() {
        return this == PLAYER1_PERFORMING || this == PLAYER2_PERFORMING || this == SCORING;
    }
}

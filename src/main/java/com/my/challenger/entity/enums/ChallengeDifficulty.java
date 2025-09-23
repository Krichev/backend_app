package com.my.challenger.entity.enums;

import lombok.Getter;

/**
 * Enum representing different difficulty levels for challenges
 */
@Getter
public enum ChallengeDifficulty {
    BEGINNER("Beginner", "Perfect for newcomers, low complexity"),
    EASY("Easy", "Simple challenges with minimal effort required"),
    MEDIUM("Medium", "Moderate difficulty requiring some dedication"),
    HARD("Hard", "Challenging tasks requiring significant effort"),
    EXPERT("Expert", "Advanced challenges for experienced participants"),
    EXTREME("Extreme", "Maximum difficulty for elite performers");

    private final String displayName;
    private final String description;

    ChallengeDifficulty(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get difficulty level as integer (1-6)
     */
    public int getLevel() {
        return ordinal() + 1;
    }

    /**
     * Convert from integer level to enum
     */
    public static ChallengeDifficulty fromLevel(int level) {
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("Difficulty level must be between 1 and 6");
        }
        return values()[level - 1];
    }

    /**
     * Check if this difficulty is easier than another
     */
    public boolean isEasierThan(ChallengeDifficulty other) {
        return this.ordinal() < other.ordinal();
    }

    /**
     * Check if this difficulty is harder than another
     */
    public boolean isHarderThan(ChallengeDifficulty other) {
        return this.ordinal() > other.ordinal();
    }
}
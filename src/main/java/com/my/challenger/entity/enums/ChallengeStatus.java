package com.my.challenger.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ChallengeStatus {
    PENDING, ACTIVE, COMPLETED, CANCELLED, OPEN;

    @JsonCreator
    public static ChallengeStatus fromString(String value) {
        if (value == null) return null;
        try {
            return ChallengeStatus.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid ChallengeStatus: '" + value + "'. Accepted values: PENDING, ACTIVE, COMPLETED, CANCELLED, OPEN"
            );
        }
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
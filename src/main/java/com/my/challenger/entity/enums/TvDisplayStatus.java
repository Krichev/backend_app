package com.my.challenger.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TvDisplayStatus {
    WAITING, CLAIMED, EXPIRED;

    @JsonCreator
    public static TvDisplayStatus fromString(String value) {
        if (value == null) return null;
        try {
            return TvDisplayStatus.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid TvDisplayStatus: '" + value + "'. Accepted values: WAITING, CLAIMED, EXPIRED"
            );
        }
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}

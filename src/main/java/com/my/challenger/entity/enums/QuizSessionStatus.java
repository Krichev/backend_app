// src/main/java/com/my/challenger/entity/enums/QuizSessionStatus.java
package com.my.challenger.entity.enums;

public enum QuizSessionStatus {
    CREATED,       // Session created but not started
    WAITING,       // Session is waiting for players to join or start
    IN_PROGRESS,   // Session is currently being played
    PAUSED,        // Session temporarily paused
    COMPLETED,     // Session finished normally
    ABANDONED,     // Session was abandoned before completion
    CANCELLED,     // Session was cancelled
    ARCHIVED       // Session completed and archived for historical purposes
}
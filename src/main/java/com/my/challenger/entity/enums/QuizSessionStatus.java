// src/main/java/com/my/challenger/entity/enums/QuizSessionStatus.java
package com.my.challenger.entity.enums;

public enum QuizSessionStatus {
    CREATED,       // Session created but not started
    IN_PROGRESS,   // Session is currently being played
    COMPLETED,     // Session finished normally
    ABANDONED,     // Session was abandoned before completion
    CANCELLED,     // Session was cancelled
    ARCHIVED       // Session completed and archived for historical purposes
}
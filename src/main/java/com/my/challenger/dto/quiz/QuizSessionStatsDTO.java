package com.my.challenger.dto.quiz;

@lombok.Data
@lombok.Builder
public class QuizSessionStatsDTO {
    private long totalSessions;
    private long completedSessions;
    private long activeSessions;
    private double completionRate;
}
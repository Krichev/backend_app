package com.my.challenger.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionStatsDTO {
    
    private Long sessionId;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer incorrectAnswers;
    private Double score;
    private Double accuracy;
    private Long duration; // in seconds
    private String status;
    private String questionSource;
    private Double averageTimePerQuestion;
    private String performanceLevel;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;
    
    // Additional statistics fields
    private Integer streakCount;
    private String difficulty;
    private String category;
    
    // Comparative statistics
    private Double percentile; // User's percentile compared to others
    private Double averageScoreComparison; // Compared to average
    
    // Method to calculate completion rate
    public Double getCompletionRate() {
        if (totalQuestions == null || totalQuestions == 0) {
            return 0.0;
        }
        int answered = (correctAnswers != null ? correctAnswers : 0) + 
                      (incorrectAnswers != null ? incorrectAnswers : 0);
        return (double) answered / totalQuestions * 100;
    }
    
    // Method to get formatted duration
    public String getFormattedDuration() {
        if (duration == null) {
            return "N/A";
        }
        long hours = duration / 3600;
        long minutes = (duration % 3600) / 60;
        long seconds = duration % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }
    
    // Method to get grade based on score
    public String getGrade() {
        if (score == null) return "N/A";
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
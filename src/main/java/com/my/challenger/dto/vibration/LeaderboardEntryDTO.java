package com.my.challenger.dto.vibration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDTO {
    private Integer rank;
    private String userId;
    private String username;
    private String avatarUrl;
    private Integer totalScore;
    private Integer gamesPlayed;
    private Double averageAccuracy;
    private Integer bestStreak;
}

package com.my.challenger.dto.vibration;

import com.my.challenger.entity.enums.LeaderboardPeriod;
import com.my.challenger.entity.enums.VibrationDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDTO {
    private LeaderboardPeriod period;
    private VibrationDifficulty difficulty;
    private List<LeaderboardEntryDTO> entries;
    private Integer currentUserRank;
    private LeaderboardEntryDTO currentUserEntry;
    private Integer page;
    private Integer totalPages;
    private Long totalEntries;
}

package com.my.challenger.dto.competitive;

import com.my.challenger.dto.wager.WagerDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitiveMatchDTO {
    private CompetitiveMatchSummaryDTO summary;
    private List<CompetitiveMatchRoundDTO> rounds;
    private WagerDTO wager;
    private String metadata;
}

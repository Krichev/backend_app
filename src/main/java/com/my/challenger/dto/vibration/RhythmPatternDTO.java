package com.my.challenger.dto.vibration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RhythmPatternDTO {
    private Integer version;
    private List<Long> onsetTimesMs;
    private List<Long> intervalsMs;
    private Double estimatedBpm;
    private String timeSignature;
    private Integer totalBeats;
    private Long trimmedStartMs;
    private Long trimmedEndMs;
    private Long originalDurationMs;
    private Double silenceThresholdDb;
    private Long minOnsetIntervalMs;
}

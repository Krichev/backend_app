package com.my.challenger.dto.penalty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyProofDTO {
    private Long id;
    private Long penaltyId;
    private Long submittedByUserId;
    private String submittedByUsername;
    private String mediaUrl;
    private String textProof;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedByUserId;
    private Boolean approved;
    private String reviewNotes;
}

package com.my.challenger.service;

import com.my.challenger.dto.penalty.AppealPenaltyRequest;
import com.my.challenger.dto.penalty.PenaltyDTO;
import com.my.challenger.dto.penalty.PenaltySummaryDTO;
import com.my.challenger.entity.enums.PenaltyStatus;
import com.my.challenger.entity.wager.Wager;
import com.my.challenger.entity.wager.WagerOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PenaltyService {

    void createPenaltyFromWager(WagerOutcome outcome, Wager wager);

    PenaltyDTO startPenalty(Long penaltyId, Long userId);

    PenaltyDTO submitProof(Long penaltyId, Long userId, String description, MultipartFile file);

    PenaltyDTO verifyPenalty(Long penaltyId, Long verifierId, boolean approved, String notes);

    PenaltyDTO appealPenalty(Long penaltyId, Long userId, AppealPenaltyRequest request);

    PenaltyDTO waivePenalty(Long penaltyId, Long waiverId);

    List<PenaltyDTO> getMyPenalties(Long userId, PenaltyStatus status);
    
    Page<PenaltyDTO> getMyPenalties(Long userId, PenaltyStatus status, Pageable pageable);

    List<PenaltyDTO> getPenaltiesToReview(Long userId);

    List<PenaltyDTO> getPenaltiesByChallenge(Long challengeId);

    PenaltyDTO getPenaltyById(Long id);

    PenaltySummaryDTO getPenaltySummary(Long userId);

    int escalateOverduePenalties();
}

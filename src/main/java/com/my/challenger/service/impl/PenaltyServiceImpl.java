package com.my.challenger.service.impl;

import com.my.challenger.dto.penalty.*;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.*;
import com.my.challenger.entity.penalty.Penalty;
import com.my.challenger.entity.penalty.PenaltyProof;
import com.my.challenger.entity.wager.Wager;
import com.my.challenger.entity.wager.WagerOutcome;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.*;
import com.my.challenger.service.PenaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PenaltyServiceImpl implements PenaltyService {

    private final PenaltyRepository penaltyRepository;
    private final PenaltyProofRepository penaltyProofRepository;
    private final UserRepository userRepository;
    private final MinioMediaStorageService mediaStorageService;
    private final EnhancedPaymentService paymentService;
    private final MediaFileRepository mediaFileRepository;
    private final com.my.challenger.service.ScreenTimeBudgetService screenTimeBudgetService;

    @Override
    @Transactional
    public void createPenaltyFromWager(WagerOutcome outcome, Wager wager) {
        if (outcome.getLoser() == null) {
            return; // No loser, no penalty
        }
        
        // Only create penalties for specific stake types
        if (wager.getStakeType() == StakeType.SOCIAL_QUEST) {
            createSocialPenalty(outcome, wager);
        } else if (wager.getStakeType() == StakeType.SCREEN_TIME) {
            createScreenTimePenalty(outcome, wager);
        }
    }

    private void createSocialPenalty(WagerOutcome outcome, Wager wager) {
        log.info("Creating SOCIAL_TASK penalty for user {}", outcome.getLoser().getId());
        
        Penalty penalty = Penalty.builder()
                .wager(wager)
                .challenge(wager.getChallenge())
                .assignedTo(outcome.getLoser())
                .assignedBy(wager.getCreator()) // Or the winner? Usually creator defines it.
                .penaltyType(PenaltyType.SOCIAL_TASK)
                .description(wager.getSocialPenaltyDescription())
                .status(PenaltyStatus.PENDING)
                .dueDate(LocalDateTime.now().plusHours(48)) // Default 48h
                .verificationMethod(PenaltyVerificationMethod.PEER_REVIEW)
                .build();
        
        penaltyRepository.save(penalty);
    }

    private void createScreenTimePenalty(WagerOutcome outcome, Wager wager) {
        log.info("Creating SCREEN_TIME_LOCK penalty for user {}", outcome.getLoser().getId());
        
        Penalty penalty = Penalty.builder()
                .wager(wager)
                .challenge(wager.getChallenge())
                .assignedTo(outcome.getLoser())
                .assignedBy(wager.getCreator())
                .penaltyType(PenaltyType.SCREEN_TIME_LOCK)
                .description("Screen time lock for " + wager.getScreenTimeMinutes() + " minutes")
                .status(PenaltyStatus.PENDING) // Logic for locking happens in Phase 3
                .dueDate(LocalDateTime.now().plusHours(24))
                .screenTimeMinutes(wager.getScreenTimeMinutes())
                .verificationMethod(PenaltyVerificationMethod.AI_VERIFICATION) // Or system verified
                .build();

        // Lock screen time
        if (wager.getScreenTimeMinutes() != null && wager.getScreenTimeMinutes() > 0) {
            try {
                screenTimeBudgetService.lockTime(outcome.getLoser().getId(), wager.getScreenTimeMinutes());
            } catch (Exception e) {
                log.error("Failed to lock screen time for user {}", outcome.getLoser().getId(), e);
                // Continue saving penalty even if locking fails? 
                // Requirement says "InsufficientScreenTimeException" is possible.
                // If locking fails (e.g. insufficient time), maybe we shouldn't create penalty?
                // Or let exception propagate.
                throw e; 
            }
        }
        
        penaltyRepository.save(penalty);
    }

    @Override
    @Transactional
    public PenaltyDTO startPenalty(Long penaltyId, Long userId) {
        Penalty penalty = getPenaltyEntity(penaltyId);
        
        if (!penalty.getAssignedTo().getId().equals(userId)) {
            throw new IllegalStateException("Only the assigned user can start the penalty");
        }
        
        if (penalty.getStatus() != PenaltyStatus.PENDING) {
            throw new IllegalStateException("Penalty is not in PENDING state");
        }
        
        penalty.setStatus(PenaltyStatus.IN_PROGRESS);
        penaltyRepository.save(penalty);
        
        return mapToDTO(penalty);
    }

    @Override
    @Transactional
    public PenaltyDTO submitProof(Long penaltyId, Long userId, String description, MultipartFile file) {
        Penalty penalty = getPenaltyEntity(penaltyId);

        if (!penalty.getAssignedTo().getId().equals(userId)) {
            throw new IllegalStateException("Only the assigned user can submit proof");
        }
        
        if (penalty.getStatus() == PenaltyStatus.VERIFIED || penalty.getStatus() == PenaltyStatus.WAIVED) {
            throw new IllegalStateException("Penalty is already finalized");
        }

        MediaFile mediaFile = null;
        if (file != null && !file.isEmpty()) {
            mediaFile = mediaStorageService.storeMedia(file, null, MediaCategory.CHALLENGE_PROOF, userId);
        }

        if (mediaFile == null && (description == null || description.trim().isEmpty())) {
            throw new IllegalArgumentException("Must provide either a file or a description");
        }

        PenaltyProof proof = PenaltyProof.builder()
                .penalty(penalty)
                .submittedBy(penalty.getAssignedTo())
                .mediaFile(mediaFile)
                .textProof(description)
                .build();
        
        penaltyProofRepository.save(proof);

        // Update penalty status
        penalty.setProofMedia(mediaFile);
        penalty.setProofDescription(description);
        penalty.setCompletedAt(LocalDateTime.now());
        
        if (penalty.getVerificationMethod() == PenaltyVerificationMethod.SELF_REPORT) {
            penalty.setStatus(PenaltyStatus.VERIFIED);
            penalty.setVerifiedAt(LocalDateTime.now());
            penalty.setVerifiedBy(penalty.getAssignedTo());
            
            // Auto-approve proof
            proof.setApproved(true);
            proof.setReviewedAt(LocalDateTime.now());
            proof.setReviewedBy(penalty.getAssignedTo());
            proof.setReviewNotes("Self-reported completion");

            // Unlock screen time if applicable
            if (penalty.getPenaltyType() == PenaltyType.SCREEN_TIME_LOCK && penalty.getScreenTimeMinutes() != null) {
                try {
                    screenTimeBudgetService.unlockTime(penalty.getAssignedTo().getId(), penalty.getScreenTimeMinutes());
                } catch (Exception e) {
                    log.error("Failed to unlock screen time for user {}", penalty.getAssignedTo().getId(), e);
                }
            }
        } else {
            penalty.setStatus(PenaltyStatus.COMPLETED); // Awaiting verification
        }
        
        penaltyRepository.save(penalty);
        penaltyProofRepository.save(proof);

        return mapToDTO(penalty);
    }

    @Override
    @Transactional
    public PenaltyDTO verifyPenalty(Long penaltyId, Long verifierId, boolean approved, String notes) {
        Penalty penalty = getPenaltyEntity(penaltyId);

        // Who can verify? AssignedBy user, or maybe admin.
        // For peer review, it should be the assignedBy user (usually wager creator/winner).
        if (!penalty.getAssignedBy().getId().equals(verifierId) && !isAdmin(verifierId)) {
             // For now assume only assigner can verify
             // In a real app, maybe the wager winner is distinct from creator, but we used creator as assigner
             throw new IllegalStateException("Not authorized to verify this penalty");
        }

        if (penalty.getStatus() != PenaltyStatus.COMPLETED && penalty.getStatus() != PenaltyStatus.APPEALED) {
             throw new IllegalStateException("Penalty is not in a state awaiting verification");
        }
        
        User verifier = userRepository.findById(verifierId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", verifierId));

        // Update latest proof
        // Ideally we pick the specific proof being verified, but simplified here to latest
        List<PenaltyProof> proofs = penaltyProofRepository.findByPenaltyIdOrderBySubmittedAtDesc(penaltyId);
        if (!proofs.isEmpty()) {
            PenaltyProof latest = proofs.get(0);
            latest.setApproved(approved);
            latest.setReviewedAt(LocalDateTime.now());
            latest.setReviewedBy(verifier);
            latest.setReviewNotes(notes);
            penaltyProofRepository.save(latest);
        }

        if (approved) {
            penalty.setStatus(PenaltyStatus.VERIFIED);
            penalty.setVerifiedAt(LocalDateTime.now());
            penalty.setVerifiedBy(verifier);

            // Unlock screen time if applicable
            if (penalty.getPenaltyType() == PenaltyType.SCREEN_TIME_LOCK && penalty.getScreenTimeMinutes() != null) {
                try {
                    screenTimeBudgetService.unlockTime(penalty.getAssignedTo().getId(), penalty.getScreenTimeMinutes());
                } catch (Exception e) {
                    log.error("Failed to unlock screen time for user {}", penalty.getAssignedTo().getId(), e);
                }
            }
        } else {
            penalty.setStatus(PenaltyStatus.IN_PROGRESS); // Send back to user
            // penalty.setCompletedAt(null); // Optional: clear completion time?
        }

        penaltyRepository.save(penalty);
        return mapToDTO(penalty);
    }

    @Override
    @Transactional
    public PenaltyDTO appealPenalty(Long penaltyId, Long userId, AppealPenaltyRequest request) {
        Penalty penalty = getPenaltyEntity(penaltyId);
        
        if (!penalty.getAssignedTo().getId().equals(userId)) {
            throw new IllegalStateException("Only the assigned user can appeal");
        }
        
        if (penalty.getStatus() == PenaltyStatus.COMPLETED || penalty.getStatus() == PenaltyStatus.VERIFIED || penalty.getStatus() == PenaltyStatus.WAIVED) {
            throw new IllegalStateException("Cannot appeal a finalized or completed penalty");
        }

        penalty.setStatus(PenaltyStatus.APPEALED);
        penalty.setAppealReason(request.getReason());
        penalty.setAppealedAt(LocalDateTime.now());
        
        penaltyRepository.save(penalty);
        return mapToDTO(penalty);
    }

    @Override
    @Transactional
    public PenaltyDTO waivePenalty(Long penaltyId, Long waiverId) {
        Penalty penalty = getPenaltyEntity(penaltyId);
        
        if (!penalty.getAssignedBy().getId().equals(waiverId) && !isAdmin(waiverId)) {
            throw new IllegalStateException("Not authorized to waive this penalty");
        }

        penalty.setStatus(PenaltyStatus.WAIVED);

        // Unlock screen time if applicable
        if (penalty.getPenaltyType() == PenaltyType.SCREEN_TIME_LOCK && penalty.getScreenTimeMinutes() != null) {
            try {
                screenTimeBudgetService.unlockTime(penalty.getAssignedTo().getId(), penalty.getScreenTimeMinutes());
            } catch (Exception e) {
                log.error("Failed to unlock screen time for user {}", penalty.getAssignedTo().getId(), e);
            }
        }

        penaltyRepository.save(penalty);
        return mapToDTO(penalty);
    }

    @Override
    public List<PenaltyDTO> getMyPenalties(Long userId, PenaltyStatus status) {
        if (status != null) {
            return penaltyRepository.findByAssignedToIdAndStatus(userId, status).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }
        return penaltyRepository.findByAssignedToId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PenaltyDTO> getMyPenalties(Long userId, PenaltyStatus status, Pageable pageable) {
        if (status != null) {
            return penaltyRepository.findByAssignedToIdAndStatus(userId, status, pageable)
                    .map(this::mapToDTO);
        }
        return penaltyRepository.findByAssignedToId(userId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public List<PenaltyDTO> getPenaltiesToReview(Long userId) {
        return penaltyRepository.findPenaltiesToReview(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PenaltyDTO> getPenaltiesByChallenge(Long challengeId) {
        return penaltyRepository.findByChallengeId(challengeId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PenaltyDTO getPenaltyById(Long id) {
        return mapToDTO(getPenaltyEntity(id));
    }

    @Override
    public PenaltySummaryDTO getPenaltySummary(Long userId) {
        return PenaltySummaryDTO.builder()
                .pendingCount(penaltyRepository.countByAssignedToIdAndStatus(userId, PenaltyStatus.PENDING))
                .inProgressCount(penaltyRepository.countByAssignedToIdAndStatus(userId, PenaltyStatus.IN_PROGRESS))
                .completedCount(penaltyRepository.countByAssignedToIdAndStatus(userId, PenaltyStatus.COMPLETED))
                .verifiedCount(penaltyRepository.countByAssignedToIdAndStatus(userId, PenaltyStatus.VERIFIED))
                // Overdue count is complicated as it depends on status AND date
                .overdueCount(penaltyRepository.findOverduePenalties(LocalDateTime.now()).stream()
                        .filter(p -> p.getAssignedTo().getId().equals(userId))
                        .count())
                .build();
    }

    @Override
    @Transactional
    public int escalateOverduePenalties() {
        List<Penalty> overdue = penaltyRepository.findOverduePenalties(LocalDateTime.now());
        int count = 0;
        
        for (Penalty p : overdue) {
            if (Boolean.TRUE.equals(p.getEscalationApplied())) continue;

            log.info("Escalating overdue penalty {}", p.getId());
            
            // Apply point deduction
            long deduction = 50L; // Default escalation fine
            try {
                paymentService.deductPoints(p.getAssignedTo(), deduction);
                p.setEscalationApplied(true);
                p.setStatus(PenaltyStatus.EXPIRED); // Or keep it pending but fined?
                // Doc says "additional consequences apply (extra point deduction)"
                // Usually expiration implies it wasn't done.
                
                // Create a record of this deduction (via paymentService or separate penalty?)
                // paymentService deductPoints handles the transaction record.
                
                penaltyRepository.save(p);
                count++;
            } catch (Exception e) {
                log.error("Failed to escalate penalty {}", p.getId(), e);
            }
        }
        return count;
    }

    private Penalty getPenaltyEntity(Long id) {
        return penaltyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty", "id", id));
    }

    private boolean isAdmin(Long userId) {
        // TODO: proper role check
        return false;
    }

    private PenaltyDTO mapToDTO(Penalty p) {
        return PenaltyDTO.builder()
                .id(p.getId())
                .wagerId(p.getWager() != null ? p.getWager().getId() : null)
                .challengeId(p.getChallenge() != null ? p.getChallenge().getId() : null)
                .assignedToUserId(p.getAssignedTo().getId())
                .assignedToUsername(p.getAssignedTo().getUsername())
                .assignedByUserId(p.getAssignedBy().getId())
                .assignedByUsername(p.getAssignedBy().getUsername())
                .penaltyType(p.getPenaltyType())
                .description(p.getDescription())
                .status(p.getStatus())
                .dueDate(p.getDueDate())
                .completedAt(p.getCompletedAt())
                .verificationMethod(p.getVerificationMethod())
                .verifiedByUserId(p.getVerifiedBy() != null ? p.getVerifiedBy().getId() : null)
                .verifiedAt(p.getVerifiedAt())
                .proofDescription(p.getProofDescription())
                .proofMediaUrl(p.getProofMedia() != null ? p.getProofMedia().getS3Url() : null)
                .screenTimeMinutes(p.getScreenTimeMinutes())
                .pointAmount(p.getPointAmount())
                .appealReason(p.getAppealReason())
                .appealedAt(p.getAppealedAt())
                .escalationApplied(p.getEscalationApplied())
                .createdAt(p.getCreatedAt())
                .build();
    }
}

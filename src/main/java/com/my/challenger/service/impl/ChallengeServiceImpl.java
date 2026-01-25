package com.my.challenger.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.CreateChallengeRequest;
import com.my.challenger.dto.UpdateChallengeRequest;
import com.my.challenger.dto.verification.VerificationHistoryDTO;
import com.my.challenger.entity.ChallengeProgress;
import com.my.challenger.entity.Task;
import com.my.challenger.entity.TaskCompletion;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.challenge.ChallengeAccess;
import com.my.challenger.entity.enums.*;
import com.my.challenger.repository.*;
import com.my.challenger.repository.specification.ChallengeSpecification;
import com.my.challenger.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final ChallengeProgressRepository challengeProgressRepository;
    private final ObjectMapper objectMapper;
    private final ChallengeAccessRepository accessRepository;
    private final PaymentService paymentService; // Inject your payment service

    @Override
    public List<Map<String, Object>> getAccessList(Long challengeId) {
        log.info("Getting access list for challenge ID: {}", challengeId);

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found with ID: " + challengeId));

        List<Map<String, Object>> accessList = new ArrayList<>();

        // Add creator with full access
        Map<String, Object> creatorAccess = new HashMap<>();
        creatorAccess.put("userId", challenge.getCreator().getId());
        creatorAccess.put("username", challenge.getCreator().getUsername());
        creatorAccess.put("accessLevel", "OWNER");
        creatorAccess.put("permissions", Arrays.asList("READ", "WRITE", "DELETE", "MANAGE"));
        accessList.add(creatorAccess);

//        // Add participants with read access
//        if (challenge.getParticipants() != null) {
//            for (User participant : challenge.getParticipants()) {
//                Map<String, Object> participantAccess = new HashMap<>();
//                participantAccess.put("userId", participant.getId());
//                participantAccess.put("username", participant.getUsername());
//                participantAccess.put("accessLevel", "PARTICIPANT");
//                participantAccess.put("permissions", Arrays.asList("READ", "SUBMIT"));
//                accessList.add(participantAccess);
//            }
//        }

        return accessList;
    }


    /**
     * Create challenge with payment and access control
     */
    @Transactional
    public ChallengeDTO createChallenge(CreateChallengeRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));

        Challenge challenge = new Challenge();
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setType(request.getType());
        challenge.setCreator(creator);
        challenge.setFrequency(request.getFrequency());
        challenge.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now());
        challenge.setEndDate(request.getEndDate());
        challenge.setStatus(request.getStatus() != null ? request.getStatus() : ChallengeStatus.ACTIVE);
        challenge.setVerificationMethod(request.getVerificationMethod());

        challenge.setDifficulty(request.getDifficulty() != null ?
                request.getDifficulty() : ChallengeDifficulty.MEDIUM);

        // Set visibility
        boolean isPublic = request.getVisibility() == VisibilityType.PUBLIC;
        challenge.setPublic(isPublic);
        challenge.setRequiresApproval(Boolean.TRUE.equals(request.getRequiresApproval()));

        // Set payment information
        setupPayment(challenge, request);

        // Save challenge first
        Challenge savedChallenge = challengeRepository.save(challenge);

        // Setup access control for private challenges
        if (!isPublic && request.getInvitedUserIds() != null && !request.getInvitedUserIds().isEmpty()) {
            grantAccessToUsers(savedChallenge, request.getInvitedUserIds(), creator);
        }

        // Auto-enroll creator as participant with task and progress
        createInitialTask(savedChallenge, creator);
        createCreatorProgress(savedChallenge, creator);

        log.info("Created challenge ID: {} by user: {} with payment type: {}",
                savedChallenge.getId(), creatorId, challenge.getPaymentType());

        return convertToDTO(savedChallenge, creatorId);
    }

    /**
     * Search challenges with access control
     */
    @Transactional(readOnly = true)
    public List<ChallengeDTO> searchChallenges(String keyword, Long userId, Pageable pageable) {
        List<Challenge> challenges = challengeRepository.searchByKeyword(keyword);

        // Filter challenges based on access
        return challenges.stream()
                .filter(challenge -> canUserAccessChallenge(challenge, userId))
                .map(challenge -> convertToDTO(challenge, userId))
                .collect(Collectors.toList());
    }

    /**
     * Get public and accessible challenges for a user
     */
    @Transactional(readOnly = true)
    public List<ChallengeDTO> getAccessibleChallenges(Long userId, Pageable pageable) {
        List<Challenge> publicChallenges = challengeRepository.findAll(pageable).getContent();

        List<ChallengeAccess> privateAccess = accessRepository.findActiveByUserId(userId);
        List<Challenge> privateChallenges = privateAccess.stream()
                .map(ChallengeAccess::getChallenge)
                .collect(Collectors.toList());

        // Combine and filter
        return publicChallenges.stream()
                .filter(Challenge::isPublic)
                .map(challenge -> convertToDTO(challenge, userId))
                .collect(Collectors.toList());
    }

    /**
     * Join a challenge with payment processing
     */
    @Transactional
    public void joinChallenge(Long challengeId, Long userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check access
        if (!canUserAccessChallenge(challenge, userId)) {
            throw new IllegalStateException("You don't have access to this challenge");
        }

        // Check if requires approval
        if (challenge.isRequiresApproval()) {
            throw new IllegalStateException("This challenge requires approval from the creator");
        }

        // Process payment if required
        if (challenge.isHasEntryFee() && challenge.getEntryFeeAmount() != null) {
            processEntryFeePayment(challenge, user);
        }

        // Create progress record
        if (!challengeProgressRepository.existsByChallengeIdAndUserId(challengeId, userId)) {
            ChallengeProgress progress = ChallengeProgress.builder()
                    .challenge(challenge)
                    .user(user)
                    .status(ProgressStatus.IN_PROGRESS)
                    .completionPercentage(0.0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            challengeProgressRepository.save(progress);
            log.info("Created progress record for user {} joining challenge {}", userId, challengeId);
        }

        // Create user task
        createUserTask(challenge, user);

    /**
     * Grant access to specific users for private challenges
     */
    @Transactional
    public void grantAccessToUsers(Challenge challenge, List<Long> userIds, User grantedBy) {
        for (Long userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            // Check if access already exists
            if (!accessRepository.hasAccess(challenge.getId(), userId)) {
                ChallengeAccess access = new ChallengeAccess();
                access.setChallenge(challenge);
                access.setUser(user);
                access.setGrantedBy(grantedBy);
                access.setGrantedAt(LocalDateTime.now());
                access.setStatus("ACTIVE");
                accessRepository.save(access);
                log.info("Granted access to user {} for challenge {}", userId, challenge.getId());
            }
        }
    }

    /**
     * Revoke access from a user
     */
    @Transactional
    public void revokeAccess(Long challengeId, Long userId, Long revokedBy) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));

        if (!challenge.getCreator().getId().equals(revokedBy)) {
            throw new IllegalStateException("Only the creator can revoke access");
        }

        ChallengeAccess access = accessRepository.findByChallengeIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Access record not found"));

        access.setStatus("REVOKED");
        accessRepository.save(access);
        log.info("Revoked access for user {} from challenge {}", userId, challengeId);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void setupPayment(Challenge challenge, CreateChallengeRequest request) {
        challenge.setPaymentType(request.getPaymentType() != null ?
                request.getPaymentType() : PaymentType.FREE);

        if (Boolean.TRUE.equals(request.getHasEntryFee())) {
            challenge.setHasEntryFee(true);
            challenge.setEntryFeeAmount(request.getEntryFeeAmount());
            challenge.setEntryFeeCurrency(request.getEntryFeeCurrency() != null ?
                    request.getEntryFeeCurrency() : CurrencyType.USD);
        }

        if (Boolean.TRUE.equals(request.getHasPrize())) {
            challenge.setHasPrize(true);
            challenge.setPrizeAmount(request.getPrizeAmount());
            challenge.setPrizeCurrency(request.getPrizeCurrency() != null ?
                    request.getPrizeCurrency() : CurrencyType.USD);
        }
    }

    private void processEntryFeePayment(Challenge challenge, User user) {
        BigDecimal amount = challenge.getEntryFeeAmount();
        CurrencyType currency = challenge.getEntryFeeCurrency();

        if (currency == CurrencyType.POINTS) {
            // Deduct points from user
            paymentService.deductPoints(user, amount.longValue());
        } else {
            // Process cash payment
            paymentService.processCashPayment(user, amount, currency);
        }

        // Add to prize pool
        challenge.addEntryFee(amount);
        challengeRepository.save(challenge);
    }

    private boolean canUserAccessChallenge(Challenge challenge, Long userId) {
        // Public challenges are accessible to all
        if (challenge.isPublic()) {
            return true;
        }

        // Creator always has access
        if (challenge.getCreator().getId().equals(userId)) {
            return true;
        }

        // Check access list for private challenges
        return accessRepository.hasAccess(challenge.getId(), userId);
    }

    private ChallengeDTO convertToDTO(Challenge challenge, Long userId) {
        ChallengeDTO dto = new ChallengeDTO();
        dto.setId(challenge.getId());
        dto.setTitle(challenge.getTitle());
        dto.setDescription(challenge.getDescription());
        dto.setType(challenge.getType());
        dto.setVisibility(challenge.isPublic() ? VisibilityType.PUBLIC : VisibilityType.PRIVATE);
        dto.setStatus(challenge.getStatus());
        dto.setCreated_at(challenge.getCreatedAt());
        dto.setUpdated_at(challenge.getUpdatedAt());
        dto.setCreator_id(challenge.getCreator().getId());
        dto.setCreatorUsername(challenge.getCreator().getUsername());
        dto.setIsPublic(challenge.isPublic());
        dto.setRequiresApproval(challenge.isRequiresApproval());
        dto.setStartDate(challenge.getStartDate());
        dto.setEndDate(challenge.getEndDate());
        dto.setFrequency(challenge.getFrequency());
        dto.setQuizConfig(challenge.getQuizConfig());

        // Payment info
        dto.setPaymentType(challenge.getPaymentType());
        dto.setHasEntryFee(challenge.isHasEntryFee());
        dto.setEntryFeeAmount(challenge.getEntryFeeAmount());
        dto.setEntryFeeCurrency(challenge.getEntryFeeCurrency());
        dto.setHasPrize(challenge.isHasPrize());
        dto.setPrizeAmount(challenge.getPrizeAmount());
        dto.setPrizeCurrency(challenge.getPrizeCurrency());
        dto.setPrizePool(challenge.getPrizePool());

        // User-specific info
        if (userId != null) {
            boolean isCreator = challenge.getCreator().getId().equals(userId);
            dto.setUserIsCreator(isCreator);
            dto.setUserHasAccess(canUserAccessChallenge(challenge, userId));
        }

        // Access count for private challenges
        if (!challenge.isPublic()) {
            long invitedCount = accessRepository.countByChallengeIdAndStatus(
                    challenge.getId(), "ACTIVE");
            dto.setInvitedUsersCount((int) invitedCount);
        }

        return dto;
    }

    @Override
    public List<ChallengeDTO> getChallenges(Map<String, Object> filters) {
        try {
            Integer page = (Integer) filters.getOrDefault("page", 0);
            Integer limit = (Integer) filters.getOrDefault("limit", 20);
            Pageable pageable = PageRequest.of(page, limit);

            List<Challenge> challenges;

            ChallengeStatus excludeStatus = null;
            if (filters.get("excludeStatus") != null) {
                try {
                    excludeStatus = ChallengeStatus.valueOf((String) filters.get("excludeStatus"));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid exclude status: {}", filters.get("excludeStatus"));
                }
            }

            // Handle participant_id filter
            if (filters.get("participant_id") != null) {
                Long participantId = (Long) filters.get("participant_id");
                challenges = getChallengesByParticipantId(participantId, pageable);
            }
            // Handle creator_id filter
            else if (filters.get("creator_id") != null) {
                Long creatorId = (Long) filters.get("creator_id");
                challenges = getChallengesByUserIdAsCreatorOrParticipant(creatorId, pageable);
            }
            // Handle general filters using Specifications (FIXED)
            else {
                // Parse enum values safely
                ChallengeType type = null;
                if (filters.get("type") != null) {
                    try {
                        type = ChallengeType.valueOf((String) filters.get("type"));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid challenge type: {}", filters.get("type"));
                    }
                }

                Boolean visibility = null;
                if (filters.get("visibility") != null) {
                    String visibilityStr = (String) filters.get("visibility");
                    visibility = "PUBLIC".equalsIgnoreCase(visibilityStr);
                }

                ChallengeStatus status = null;
                if (filters.get("status") != null) {
                    try {
                        status = ChallengeStatus.valueOf((String) filters.get("status"));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid challenge status: {}", filters.get("status"));
                    }
                }

                String targetGroup = (String) filters.get("targetGroup");

                // Use Specification to build dynamic query (SOLVES POSTGRES ENUM ISSUE)
                Specification<Challenge> spec = ChallengeSpecification.withFilters(
                        type, visibility, status, targetGroup, excludeStatus
                );

                challenges = challengeRepository.findAll(spec, pageable).getContent();
            }

            // Apply excludeStatus filter for repository methods that don't support it via Specification
            if (excludeStatus != null) {
                final ChallengeStatus finalExcludeStatus = excludeStatus;
                challenges = challenges.stream()
                        .filter(c -> !finalExcludeStatus.equals(c.getStatus()))
                        .collect(Collectors.toList());
            }

            // Convert to DTOs
            Long requestUserId = (Long) filters.get("requestUserId");

            return challenges.stream()
                    .map(challenge -> convertToDTO(challenge, requestUserId))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting challenges with filters: {}", filters, e);
            throw new RuntimeException("Failed to get challenges: " + e.getMessage(), e);
        }
    }


    // Keep your existing methods unchanged
    private List<Challenge> getChallengesByParticipantId(Long participantId, Pageable pageable) {
        return challengeRepository.findChallengesByParticipantId(participantId, pageable);
    }

    private List<Challenge> getChallengesByUserIdAsCreatorOrParticipant(Long userId, Pageable pageable) {
        return challengeRepository.findChallengesByUserIdAsCreatorOrParticipant(userId, pageable);
    }

    @Override
    public ChallengeDTO getChallengeById(Long id, Long requestUserId) {
        Challenge challenge = findChallengeById(id);
        return convertToDTO(challenge, requestUserId);
    }

    @Override
    public Optional<Challenge> getChallengeById(Long id) {
        Challenge challenge = findChallengeById(id);
        if(challenge == null) {
            return Optional.empty();
        }
        return Optional.of(challenge);
    }


    @Override
    @Transactional
    public ChallengeDTO updateChallenge(Long id, UpdateChallengeRequest request, Long requestUserId) {
        Challenge challenge = findChallengeById(id);

        // Verify ownership
        validateChallengeOwnership(id, requestUserId);

        // Update basic fields
        if (request.getTitle() != null) {
            challenge.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            challenge.setDescription(request.getDescription());
        }
        if (request.getEndDate() != null) {
            challenge.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            try {
                ChallengeStatus newStatus = request.getStatus();
                challenge.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status in update request: {}", request.getStatus());
            }
        }

        // Update verification method if provided
        if (request.getVerificationMethod() != null) {
            try {
                VerificationMethod newMethod = VerificationMethod.valueOf(request.getVerificationMethod());
                challenge.setVerificationMethod(newMethod);

                // Clear existing verification details
                if (challenge.getVerificationDetails() != null) {
                    challenge.getVerificationDetails().clear();
                }

            } catch (IllegalArgumentException e) {
                log.warn("Invalid verification method in update request: {}", request.getVerificationMethod());
            }
        }

        Challenge updatedChallenge = challengeRepository.save(challenge);
        return convertToDTO(updatedChallenge, requestUserId);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        Challenge challenge = findChallengeById(id);

        // Mark as cancelled instead of physical deletion to maintain data integrity
        challenge.setStatus(ChallengeStatus.CANCELLED);
        challengeRepository.save(challenge);

        log.info("Challenge with ID {} has been marked as cancelled", id);
    }


    @Override
    @Transactional
    public void submitChallengeCompletion(Long challengeId, Long userId, Map<String, Object> proofData, String notes) {
        Challenge challenge = findChallengeById(challengeId);
        User user = findUserById(userId);

        // Find active task for this challenge and user
        Task task = taskRepository.findFirstByChallengeIdAndAssignedToAndStatus(challengeId, userId, TaskStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("No active task found for this challenge"));

        // Create task completion record
        TaskCompletion completion = new TaskCompletion();
        completion.setTask(task);
        completion.setTaskId(task.getId());
        completion.setUser(user);
        completion.setUserId(userId);
        completion.setCompletionDate(LocalDateTime.now());

        // Set status based on verification method
        if (challenge.getVerificationMethod() != null &&
                challenge.getVerificationMethod() == VerificationMethod.MANUAL) {
            completion.setStatus(CompletionStatus.PENDING);
        } else {
            completion.setStatus(CompletionStatus.VERIFIED);
            task.setStatus(TaskStatus.COMPLETED);
        }

        completion.setNotes(notes);

        // Store proof data as JSON
        if (proofData != null && !proofData.isEmpty()) {
            try {
                String proofJson = objectMapper.writeValueAsString(proofData);
                completion.setVerificationProof(proofJson);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize proof data for task completion", e);
            }
        }

        taskCompletionRepository.save(completion);

        // Update challenge progress
        updateChallengeProgress(challenge, user);

        log.info("User {} submitted completion for challenge {}", userId, challengeId);
    }

    @Override
    @Transactional
    public void verifyChallengeCompletion(Long challengeId, Long userId, boolean approved) {
        Challenge challenge = findChallengeById(challengeId);
        User user = findUserById(userId);

        // Find the most recent task completion for this challenge and user
        TaskCompletion completion = taskCompletionRepository
                .findFirstByTaskChallengeIdAndUserIdOrderByCompletionDateDesc(challengeId, userId)
                .orElseThrow(() -> new IllegalStateException("No completion record found for user " + userId + " on challenge " + challengeId));

        // Update the verification status
        completion.setStatus(approved ? CompletionStatus.VERIFIED : CompletionStatus.REJECTED);
        completion.setVerificationDate(LocalDateTime.now());

        taskCompletionRepository.save(completion);

        if (approved) {
            // Update the task status to completed
            Task task = completion.getTask();
            if (task != null) {
                task.setStatus(TaskStatus.COMPLETED);
                taskRepository.save(task);
            }

            // Update challenge progress
            updateChallengeProgress(challenge, user);

            log.info("Challenge completion approved for user {} on challenge {}", userId, challengeId);
        } else {
            log.info("Challenge completion rejected for user {} on challenge {}", userId, challengeId);
        }
    }

    @Override
    public List<ChallengeDTO> searchChallenges(String query, Long requestUserId) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<Challenge> challenges = challengeRepository.searchByKeyword(query.trim());
            return challenges.stream()
                    .map(challenge -> convertToDTO(challenge, requestUserId))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching challenges with query: {}", query, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getVerificationHistory(Long challengeId, Long requestUserId) {
        Challenge challenge = findChallengeById(challengeId);

        // Verify user has rights to see verification history (creator or admin)
        validateChallengeVerificationRights(challengeId, requestUserId);

        // Get all task completions for this challenge across all statuses
        List<TaskCompletion> allCompletions = new java.util.ArrayList<>();

        // Get completions by status
        allCompletions.addAll(taskCompletionRepository.findByTaskChallengeIdAndStatus(challengeId, CompletionStatus.PENDING));
        allCompletions.addAll(taskCompletionRepository.findByTaskChallengeIdAndStatus(challengeId, CompletionStatus.VERIFIED));
        allCompletions.addAll(taskCompletionRepository.findByTaskChallengeIdAndStatus(challengeId, CompletionStatus.REJECTED));
        allCompletions.addAll(taskCompletionRepository.findByTaskChallengeIdAndStatus(challengeId, CompletionStatus.SUBMITTED));

        // Sort by completion date descending
        allCompletions.sort((a, b) -> {
            if (a.getCompletionDate() == null && b.getCompletionDate() == null) return 0;
            if (a.getCompletionDate() == null) return 1;
            if (b.getCompletionDate() == null) return -1;
            return b.getCompletionDate().compareTo(a.getCompletionDate());
        });

        return allCompletions.stream()
                .map(completion -> {
                    Map<String, Object> details = new HashMap<>();

                    try {
                        if (completion.getVerificationProof() != null && !completion.getVerificationProof().isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsedMap = objectMapper.readValue(completion.getVerificationProof(),
                                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                                    });
                            details = parsedMap;
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing verification proof", e);
                        details.put("rawProof", completion.getVerificationProof());
                    }

                    VerificationHistoryDTO dto = VerificationHistoryDTO.builder()
                            .challengeId(challengeId)
                            .userId(completion.getUserId())
                            .challengeTitle(challenge.getTitle())
                            .userName(completion.getUser() != null ? completion.getUser().getUsername() : "Unknown")
                            .completionDate(completion.getCompletionDate() != null ?
                                    completion.getCompletionDate().toString() : null)
                            .verificationDate(completion.getVerificationDate() != null ?
                                    completion.getVerificationDate().toString() : null)
                            .status(completion.getStatus().toString())
                            .notes(completion.getNotes())
                            .details(details)
                            .build();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = objectMapper.convertValue(dto,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                            });
                    return resultMap;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void validateChallengeOwnership(Long challengeId, Long userId) {
        Challenge challenge = findChallengeById(challengeId);

        if (!challenge.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("User does not have permission to modify this challenge");
        }
    }

    @Override
    public void validateChallengeVerificationRights(Long challengeId, Long userId) {
        Challenge challenge = findChallengeById(challengeId);

        if (!challenge.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("User does not have permission to verify completions for this challenge");
        }
    }

    /**
     * Helper method to find challenge by ID
     */
    private Challenge findChallengeById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found with ID: " + id));
    }

    /**
     * Helper method to find user by ID
     */
    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    /**
     * Helper method to create initial task for a challenge creator
     */
    private void createInitialTask(Challenge challenge, User creator) {
        Task task = new Task();
        task.setTitle(challenge.getTitle());
        task.setDescription(challenge.getDescription());
        task.setType(challenge.getFrequency() != null ?
                TaskType.valueOf(challenge.getFrequency().name()) :
                TaskType.ONE_TIME);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setVerificationMethod(challenge.getVerificationMethod());
        task.setStartDate(challenge.getStartDate());
        task.setEndDate(challenge.getEndDate());
        task.setChallenge(challenge);
        task.setAssignedToUser(creator);
        task.setAssignedTo(creator.getId());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        taskRepository.save(task);

        log.debug("Created initial task for challenge {} and creator {}", challenge.getId(), creator.getId());
    }

    /**
     * Helper method to create task for a user who joined the challenge
     */
    private void createUserTask(Challenge challenge, User user) {
        Task task = new Task();
        task.setTitle(challenge.getTitle());
        task.setDescription(challenge.getDescription());
        task.setType(challenge.getFrequency() != null ?
                TaskType.valueOf(challenge.getFrequency().name()) :
                TaskType.ONE_TIME);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setVerificationMethod(challenge.getVerificationMethod());
        task.setStartDate(LocalDateTime.now());
        task.setEndDate(challenge.getEndDate());
        task.setChallenge(challenge);
        task.setAssignedToUser(user);
        task.setAssignedTo(user.getId());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        taskRepository.save(task);

        log.debug("Created user task for challenge {} and user {}", challenge.getId(), user.getId());
    }

    /**
     * Helper method to create progress record for challenge creator
     */
    private void createCreatorProgress(Challenge challenge, User creator) {
        ChallengeProgress progress = new ChallengeProgress();
        progress.setChallenge(challenge);
        progress.setUser(creator);
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setCompletionPercentage(0.0);
        progress.setCreatedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        challengeProgressRepository.save(progress);

        log.debug("Created progress record for challenge {} and creator {}", challenge.getId(), creator.getId());
    }

    /**
     * Helper method to update challenge progress
     */
    private void updateChallengeProgress(Challenge challenge, User user) {
        // Get existing progress or create new one (defensive for quiz challenges)
        ChallengeProgress progress = challengeProgressRepository.findByChallengeIdAndUserId(challenge.getId(), user.getId())
                .orElseGet(() -> {
                    log.info("Auto-creating progress record for user {} on challenge {} (implicit join via quiz)",
                            user.getId(), challenge.getId());
                    return createProgressRecord(challenge, user);
                });

        // Count total and completed tasks
        long totalTasks = taskRepository.countByChallengeIdAndAssignedTo(challenge.getId(), user.getId());
        long completedTasks = taskRepository.countByChallengeIdAndAssignedToAndStatus(
                challenge.getId(), user.getId(), TaskStatus.COMPLETED);

        // Calculate completion percentage
        double completionPercentage = totalTasks > 0 ?
                ((double) completedTasks / totalTasks) * 100 : 0;
        progress.setCompletionPercentage(completionPercentage);

        // Update status if all tasks are completed
        if (completedTasks == totalTasks && totalTasks > 0) {
            progress.setStatus(ProgressStatus.COMPLETED);
        }

        progress.setUpdatedAt(LocalDateTime.now());
        challengeProgressRepository.save(progress);

        log.debug("Updated progress for user {} on challenge {}: {}%",
                user.getId(), challenge.getId(), completionPercentage);
    }

    private ChallengeProgress createProgressRecord(Challenge challenge, User user) {
        ChallengeProgress progress = ChallengeProgress.builder()
                .challenge(challenge)
                .user(user)
                .status(ProgressStatus.IN_PROGRESS)
                .completionPercentage(0.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return challengeProgressRepository.save(progress);
    }

}
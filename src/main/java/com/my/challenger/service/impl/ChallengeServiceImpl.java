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
import com.my.challenger.entity.challenge.LocationCoordinates;
import com.my.challenger.entity.challenge.PhotoVerificationDetails;
import com.my.challenger.entity.challenge.VerificationDetails;
import com.my.challenger.entity.enums.*;
import com.my.challenger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    public List<ChallengeDTO> getChallenges(Map<String, Object> filters) {
        Integer page = (Integer) filters.getOrDefault("page", 0);
        Integer limit = (Integer) filters.getOrDefault("limit", 20);
        Pageable pageable = PageRequest.of(page, limit);

        List<Challenge> challenges;

        try {
            // Apply filters based on the provided parameters
            if (filters.get("participant_id") != null) {
                Long participantId = (Long) filters.get("participant_id");
                challenges = getChallengesByParticipantId(participantId, pageable);
            } else if (filters.get("creator_id") != null) {
                Long creatorId = (Long) filters.get("creator_id");
                challenges = getChallengesByUserIdAsCreatorOrParticipant(creatorId, pageable);
            } else {
                // Apply other filters like type, visibility, status
                ChallengeType type = filters.get("type") != null ?
                        ChallengeType.valueOf((String) filters.get("type")) : null;

                Boolean visibility = filters.get("visibility") != null ?
                        "PUBLIC".equals(filters.get("visibility")) : null;

                ChallengeStatus status = filters.get("status") != null ?
                        ChallengeStatus.valueOf((String) filters.get("status")) : null;

                String targetGroup = (String) filters.get("targetGroup");

                challenges = challengeRepository.findWithFilters(
                        type,
                        visibility,
                        status,
                        targetGroup,
                        pageable
                );
            }

            Long requestUserId = (Long) filters.get("requestUserId");

            return challenges.stream()
                    .map(challenge -> convertToDTO(challenge, requestUserId))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting challenges with filters: {}", filters, e);
            throw new RuntimeException("Failed to retrieve challenges", e);
        }
    }

    /**
     * FIXED METHOD: Get challenges by participant ID using new progress system
     */
    private List<Challenge> getChallengesByParticipantId(Long participantId, Pageable pageable) {
        try {
            // Use the new progress system instead of old participants relationship
            List<ChallengeProgress> progressList = challengeProgressRepository.findByUserId(participantId);

            return progressList.stream()
                    .map(ChallengeProgress::getChallenge)
                    .distinct() // Remove duplicates if any
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding challenges by participant ID: {}", participantId, e);
            // Fallback to repository method if it exists
            return challengeRepository.findChallengesByParticipantId(participantId, pageable);
        }
    }

    /**
     * FIXED METHOD: Get challenges by user ID as creator or participant
     */
    private List<Challenge> getChallengesByUserIdAsCreatorOrParticipant(Long userId, Pageable pageable) {
        try {
            // Get challenges where user is creator
            List<Challenge> createdChallenges = challengeRepository.findByCreatorId(userId, pageable);

            // Get challenges where user is participant (using progress system)
            List<Challenge> participatedChallenges = getChallengesByParticipantId(userId, pageable);

            // Combine and remove duplicates
            List<Challenge> allChallenges = createdChallenges.stream()
                    .collect(Collectors.toList());

            participatedChallenges.stream()
                    .filter(challenge -> !allChallenges.contains(challenge))
                    .forEach(allChallenges::add);

            return allChallenges.stream()
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding challenges by user ID as creator or participant: {}", userId, e);
            // Fallback to repository method if it exists
            return challengeRepository.findChallengesByUserIdAsCreatorOrParticipant(userId, pageable);
        }
    }

    @Override
    public ChallengeDTO getChallengeById(Long id, Long requestUserId) {
        Challenge challenge = findChallengeById(id);
        return convertToDTO(challenge, requestUserId);
    }

    @Override
    @Transactional
    public ChallengeDTO createChallenge(CreateChallengeRequest request, Long creatorId) {
        User creator = findUserById(creatorId);

        Challenge challenge = new Challenge();
        challenge.setType(request.getType());
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setCreator(creator);
        challenge.setPublic(request.getVisibility().equals("PUBLIC"));
        challenge.setStartDate(request.getStartDate() != null ?
                request.getStartDate() : LocalDateTime.now());
        challenge.setEndDate(request.getEndDate() != null ?
                request.getEndDate() : null);
        challenge.setFrequency(request.getFrequency());
        challenge.setStatus(request.getStatus() != null ?
                request.getStatus() : ChallengeStatus.ACTIVE);

        // Set verification method
        if (request.getVerificationMethod() != null) {
            try {
                VerificationMethod verificationMethod = request.getVerificationMethod();
                challenge.setVerificationMethod(verificationMethod);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid verification method: {}", request.getVerificationMethod());
                challenge.setVerificationMethod(VerificationMethod.MANUAL);
            }
        }

        // Save challenge first
        Challenge savedChallenge = challengeRepository.save(challenge);

        // Create initial task for the creator
        createInitialTask(savedChallenge, creator);

        // Create progress record for the creator
        createCreatorProgress(savedChallenge, creator);

        log.info("Created challenge with ID: {} by user: {}", savedChallenge.getId(), creatorId);
        return convertToDTO(savedChallenge, creatorId);
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
    public void joinChallenge(Long challengeId, Long userId) {
        Challenge challenge = findChallengeById(challengeId);
        User user = findUserById(userId);

        // Check if user already joined
        boolean alreadyJoined = challengeProgressRepository.existsByChallengeIdAndUserId(challengeId, userId);
        if (alreadyJoined) {
            throw new IllegalStateException("User has already joined this challenge");
        }

        // Check if challenge is still active and joinable
        if (challenge.getStatus() != ChallengeStatus.ACTIVE) {
            throw new IllegalStateException("Cannot join inactive challenge");
        }

        // Create challenge progress entry
        ChallengeProgress progress = new ChallengeProgress();
        progress.setChallenge(challenge);
        progress.setUser(user);
        progress.setStatus(ProgressStatus.IN_PROGRESS);
        progress.setCompletionPercentage(0.0);
        progress.setCreatedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        challengeProgressRepository.save(progress);

        // Create task for the user
        createUserTask(challenge, user);

        log.info("User {} joined challenge {}", userId, challengeId);
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
        task.setStatus(TaskStatus.NOT_STARTED);
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
        ChallengeProgress progress = challengeProgressRepository.findByChallengeIdAndUserId(challenge.getId(), user.getId())
                .orElseThrow(() -> new IllegalStateException("User has not joined this challenge"));

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

    /**
     * Helper method to convert Challenge entity to DTO
     */
    private ChallengeDTO convertToDTO(Challenge challenge, Long requestUserId) {
        ChallengeDTO dto = new ChallengeDTO();
        dto.setId(challenge.getId());
        dto.setTitle(challenge.getTitle());
        dto.setDescription(challenge.getDescription());
        dto.setType(challenge.getType());
        dto.setVisibility(challenge.isPublic() ?
                VisibilityType.PUBLIC : VisibilityType.PRIVATE);
        dto.setStatus(challenge.getStatus());
        dto.setCreated_at(challenge.getStartDate()); // Using startDate as created_at
        dto.setCreator_id(challenge.getCreator().getId());
        dto.setCreatorUsername(challenge.getCreator().getUsername());
        dto.setFrequency(challenge.getFrequency());
        dto.setStartDate(challenge.getStartDate());
        dto.setEndDate(challenge.getEndDate());

        // Set user-specific flags
        if (requestUserId != null) {
            boolean isCreator = challenge.getCreator().getId().equals(requestUserId);
            dto.setUserIsCreator(isCreator);
            dto.setUserRole(isCreator ? "CREATOR" : "PARTICIPANT");

            // Check if user has joined this challenge
            boolean hasJoined = challengeProgressRepository.existsByChallengeIdAndUserId(challenge.getId(), requestUserId);
            dto.setUserHasJoined(hasJoined || isCreator);
        }

        // Get participant count using new progress system
        Long participantCount = challengeProgressRepository.countByChallengeId(challenge.getId());
        dto.setParticipantCount(participantCount.intValue());

        // Set verification method as string (enum name)
        if (challenge.getVerificationMethod() != null) {
            dto.setVerificationMethod(challenge.getVerificationMethod().toString());
        }

        return dto;
    }
}
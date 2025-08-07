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

        // Apply filters based on the provided parameters
        if (filters.get("participant_id") != null) {
            Long participantId = (Long) filters.get("participant_id");
            challenges = challengeRepository.findChallengesByParticipantId(participantId, pageable);
        } else if (filters.get("creator_id") != null) {
            Long creatorId = (Long) filters.get("creator_id");
            challenges = challengeRepository.findChallengesByUserIdAsCreatorOrParticipant(creatorId, pageable);
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

        // Handle verification method - UPDATED to use new verificationDetails field
        if (request.getVerificationMethod() != null) {
            VerificationMethod verificationMethod = request.getVerificationMethod();
            challenge.setVerificationMethod(verificationMethod);

            // Only create VerificationDetails for challenges that need them
            if (verificationMethod == VerificationMethod.PHOTO ||
                    verificationMethod == VerificationMethod.LOCATION) {

                if (request.getVerificationDetails() != null) {
                    VerificationDetails verificationDetails = createVerificationDetails(
                            request.getVerificationDetails(), verificationMethod);
                    verificationDetails.setChallenge(challenge.getId());
                    challenge.setVerificationDetails(Collections.singletonList(verificationDetails));
                }
            }
            // For QUIZ, MANUAL, FITNESS_API, ACTIVITY - no VerificationDetails needed
        }

        Challenge savedChallenge = challengeRepository.save(challenge);
        createInitialTask(savedChallenge, creator);

        return convertToDTO(savedChallenge, creatorId);
    }

    /**
     * Create VerificationDetails only when needed (PHOTO/LOCATION verification)
     */
    private VerificationDetails createVerificationDetails(
            Map<String, Object> verificationData,
            VerificationMethod method) {

        if (verificationData == null || verificationData.isEmpty()) {
            return null;
        }

        VerificationDetails.VerificationDetailsBuilder builder = VerificationDetails.builder();

        if (method == VerificationMethod.PHOTO) {
            PhotoVerificationDetails photoDetails = new PhotoVerificationDetails();
            photoDetails.setDescription((String) verificationData.getOrDefault("description", ""));
            photoDetails.setRequiresPhotoComparison(
                    (Boolean) verificationData.getOrDefault("requiresComparison", false));
            photoDetails.setVerificationMode(
                    (String) verificationData.getOrDefault("verificationMode", "standard"));

            builder.photoDetails(photoDetails);
            builder.activityType("PHOTO_VERIFICATION");
        }

        if (method == VerificationMethod.LOCATION) {
            LocationCoordinates coordinates = new LocationCoordinates();

            // Handle different number types from JSON
            Object latObj = verificationData.get("latitude");
            Object lngObj = verificationData.get("longitude");
            Object radiusObj = verificationData.getOrDefault("radius", 100);

            Double latitude = latObj instanceof Number ? ((Number) latObj).doubleValue() :
                    Double.parseDouble(latObj.toString());
            Double longitude = lngObj instanceof Number ? ((Number) lngObj).doubleValue() :
                    Double.parseDouble(lngObj.toString());

            coordinates.setLatitude(latitude);
            coordinates.setLongitude(longitude);

            builder.locationCoordinates(coordinates);

            Double radius = radiusObj instanceof Number ? ((Number) radiusObj).doubleValue() :
                    Double.parseDouble(radiusObj.toString());
            builder.radius(radius);
            builder.activityType("LOCATION_VERIFICATION");
        }

        return builder.build();
    }

    @Override
    @Transactional
    public ChallengeDTO updateChallenge(Long id, UpdateChallengeRequest request, Long requestUserId) {
        Challenge challenge = findChallengeById(id);

        // Validate ownership
        validateChallengeOwnership(id, requestUserId);

        // Update fields if provided
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            challenge.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            challenge.setDescription(request.getDescription());
        }

        if (request.getType() != null) {
            challenge.setType(request.getType());
        }

        if (request.getVisibility() != null) {
            challenge.setPublic(request.getVisibility() == VisibilityType.PUBLIC);
        }

        if (request.getStatus() != null) {
            challenge.setStatus(request.getStatus());
        }

        if (request.getFrequency() != null) {
            challenge.setFrequency(request.getFrequency());
        }

        if (request.getStartDate() != null) {
            challenge.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            challenge.setEndDate(request.getEndDate());
        }

        // Handle verification method updates
        if (request.getVerificationMethod() != null) {
            try {
                VerificationMethod newMethod = VerificationMethod.valueOf(request.getVerificationMethod());
                challenge.setVerificationMethod(newMethod);

                // Clear existing verification details
                challenge.getVerificationDetails().clear();

                // Add new verification details if needed
                // Note: UpdateChallengeRequest would need verificationDetails field for this to work fully

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
            // Manual verification requires admin approval
            completion.setStatus(CompletionStatus.SUBMITTED);
        } else {
            // Auto-verification for other methods
            completion.setStatus(CompletionStatus.VERIFIED);

            // Update task status
            task.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(task);

            // Update challenge progress
            updateChallengeProgress(challenge, user);
        }

        // Set verification proof and notes
        if (proofData != null) {
            try {
                String proofJson = objectMapper.writeValueAsString(proofData);
                completion.setVerificationProof(proofJson);
            } catch (JsonProcessingException e) {
                log.error("Error processing proof data", e);
            }
        }

        completion.setNotes(notes);
        completion.setCreatedAt(LocalDateTime.now());

        taskCompletionRepository.save(completion);

        log.info("Challenge completion submitted for user {} on challenge {}", userId, challengeId);
    }

    @Override
    @Transactional
    public void verifyChallengeCompletion(Long challengeId, Long userId, boolean approved) {
        Challenge challenge = findChallengeById(challengeId);

        // Find the most recent task completion for this challenge and user
        TaskCompletion completion = taskCompletionRepository.findFirstByTaskChallengeIdAndUserIdOrderByCompletionDateDesc(challengeId, userId)
                .orElseThrow(() -> new IllegalStateException("No completion record found"));

        // Update the status
        completion.setStatus(approved ? CompletionStatus.VERIFIED : CompletionStatus.REJECTED);
        completion.setVerificationDate(LocalDateTime.now());

        taskCompletionRepository.save(completion);

        if (approved) {
            // Update the task status
            Task task = completion.getTask();
            task.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(task);

            // Update challenge progress
            updateChallengeProgress(challenge, completion.getUser());
        }

        log.info("Challenge completion {} for user {} on challenge {}",
                approved ? "approved" : "rejected", userId, challengeId);
    }

    @Override
    public List<ChallengeDTO> searchChallenges(String query, Long requestUserId) {
        List<Challenge> challenges = challengeRepository.searchByKeyword(query);
        return challenges.stream()
                .map(challenge -> convertToDTO(challenge, requestUserId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getVerificationHistory(Long challengeId, Long userId) {
        Challenge challenge = findChallengeById(challengeId);
        User user = findUserById(userId);

        List<TaskCompletion> completions = taskCompletionRepository
                .findByTaskChallengeIdAndUserIdOrderByCompletionDateDesc(challengeId, userId);

        return completions.stream()
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
                            .userId(userId)
                            .challengeTitle(challenge.getTitle())
                            .userName(user.getUsername())
                            .completionDate(completion.getCompletionDate().toString())
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

        // Get participant count
        Long participantCount = challengeProgressRepository.countByChallengeId(challenge.getId());
        dto.setParticipantCount(participantCount.intValue());

        // Set verification method as string (enum name)
        if (challenge.getVerificationMethod() != null) {
            dto.setVerificationMethod(challenge.getVerificationMethod().toString());
        }

        return dto;
    }
}
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
import com.my.challenger.entity.challenge.VerificationDetails;
import com.my.challenger.entity.enums.ChallengeStatus;
import com.my.challenger.entity.enums.CompletionStatus;
import com.my.challenger.entity.enums.ProgressStatus;
import com.my.challenger.entity.enums.TaskStatus;
import com.my.challenger.repository.ChallengeProgressRepository;
import com.my.challenger.repository.ChallengeRepository;
import com.my.challenger.repository.TaskCompletionRepository;
import com.my.challenger.repository.TaskRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            challenges = challengeRepository.findByCreatorId(creatorId, pageable);
        } else {
            // Apply other filters like type, visibility, status
            challenges = challengeRepository.findWithFilters(
                    (String) filters.get("type"),
                    (String) filters.get("visibility"),
                    (String) filters.get("status"),
                    (String) filters.get("targetGroup"),
                    pageable
            );
        }
        
        return challenges.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ChallengeDTO getChallengeById(Long id) {
        Challenge challenge = findChallengeById(id);
        return convertToDTO(challenge);
    }

    @Override
    @Transactional
    public ChallengeDTO createChallenge(CreateChallengeRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));
        
        Challenge challenge = new Challenge();
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setType(request.getType());
        challenge.setCreator(creator);
        challenge.setPublic(request.getVisibility().toString().equals("PUBLIC"));
        challenge.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now());
        challenge.setEndDate(request.getEndDate());
        challenge.setFrequency(request.getFrequency());
        challenge.setStatus(request.getStatus() != null ? request.getStatus() : ChallengeStatus.ACTIVE);
        
        // Parse verification method details if available
        if (request.getVerificationMethod() != null && !request.getVerificationMethod().isEmpty()) {
            try {
                // Just storing as string, would be parsed in a real implementation
                // A real implementation would deserialize into VerificationDetails object
                challenge.setVerificationDetails(Collections.singletonList(new VerificationDetails()));
            } catch (Exception e) {
                log.error("Error parsing verification method", e);
                throw new IllegalArgumentException("Invalid verification method format");
            }
        }
        
        Challenge savedChallenge = challengeRepository.save(challenge);
        
        // Create initial task for the challenge
        createInitialTask(savedChallenge, creator);
        
        return convertToDTO(savedChallenge);
    }

    @Override
    @Transactional
    public ChallengeDTO updateChallenge(Long id, UpdateChallengeRequest request) {
        Challenge challenge = findChallengeById(id);
        
        if (request.getTitle() != null) {
            challenge.setTitle(request.getTitle());
        }
        
        if (request.getDescription() != null) {
            challenge.setDescription(request.getDescription());
        }
        
        if (request.getType() != null) {
            challenge.setType(request.getType());
        }
        
        if (request.getVisibility() != null) {
            challenge.setPublic(request.getVisibility().toString().equals("PUBLIC"));
        }
        
        if (request.getStatus() != null) {
            challenge.setStatus(request.getStatus());
        }
        
        if (request.getStartDate() != null) {
            challenge.setStartDate(request.getStartDate());
        }
        
        if (request.getEndDate() != null) {
            challenge.setEndDate(request.getEndDate());
        }
        
        if (request.getFrequency() != null) {
            challenge.setFrequency(request.getFrequency());
        }
        
        // Update verification method details if available
        if (request.getVerificationMethod() != null) {
            try {
                // Just storing as string, would be parsed in a real implementation
                // A real implementation would deserialize into VerificationDetails object
                challenge.setVerificationDetails(Collections.singletonList(new VerificationDetails()));
            } catch (Exception e) {
                log.error("Error parsing verification method", e);
                throw new IllegalArgumentException("Invalid verification method format");
            }
        }
        
        Challenge updatedChallenge = challengeRepository.save(challenge);
        return convertToDTO(updatedChallenge);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        Challenge challenge = findChallengeById(id);
        
        // Mark as cancelled instead of physical deletion
        challenge.setStatus(ChallengeStatus.CANCELLED);
        challengeRepository.save(challenge);
        
        // Alternative: Physical deletion (uncommenting below)
        // First, delete related entities like tasks, progress, etc.
        // taskRepository.deleteAllByChallengeId(id);
        // challengeProgressRepository.deleteAllByChallengeId(id);
        // Then delete the challenge
        // challengeRepository.delete(challenge);
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
                challenge.getVerificationMethod().toString().equals("MANUAL")) {
            // Manual verification requires admin approval
            completion.setStatus(CompletionStatus.SUBMITTED);
        } else {
            // Auto-verification (would be handled by verification service in real implementation)
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
    }

    @Override
    public List<ChallengeDTO> searchChallenges(String query) {
        List<Challenge> challenges = challengeRepository.searchByKeyword(query);
        return challenges.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getVerificationHistory(Long challengeId, Long userId) {
        Challenge challenge = findChallengeById(challengeId);
        User user = findUserById(userId);

        List<TaskCompletion> completions = taskCompletionRepository.findByTaskChallengeIdAndUserIdOrderByCompletionDateDesc(challengeId, userId);

        return completions.stream()
                .map(completion -> {
                    Map<String, Object> details = new HashMap<>();

                    try {
                        if (completion.getVerificationProof() != null && !completion.getVerificationProof().isEmpty()) {
                            // Try to parse the proof as JSON
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsedMap = objectMapper.readValue(completion.getVerificationProof(),
                                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
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
                            .verificationDate(completion.getVerificationDate() != null ? completion.getVerificationDate().toString() : null)
                            .status(completion.getStatus().toString())
                            .notes(completion.getNotes())
                            .details(details)
                            .build();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = objectMapper.convertValue(dto,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    return resultMap;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void validateChallengeOwnership(Long challengeId, Long userId) {
        Challenge challenge = findChallengeById(challengeId);
        
        if (!challenge.getCreator().getId().equals(userId)) {
            // In a real app, you might check if the user has admin rights too
            throw new IllegalStateException("User does not have permission to modify this challenge");
        }
    }

    @Override
    public void validateChallengeVerificationRights(Long challengeId, Long userId) {
        Challenge challenge = findChallengeById(challengeId);
        
        if (!challenge.getCreator().getId().equals(userId)) {
            // In a real app, you might check if the user has admin or moderator rights too
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
     * Helper method to create initial task for a challenge
     */
    private void createInitialTask(Challenge challenge, User creator) {
        Task task = new Task();
        task.setTitle(challenge.getTitle());
        task.setDescription(challenge.getDescription());
        task.setType(challenge.getFrequency() != null ? 
                com.my.challenger.entity.enums.TaskType.valueOf(challenge.getFrequency().name()) : 
                com.my.challenger.entity.enums.TaskType.ONE_TIME);
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
    }

    /**
     * Helper method to create task for a user who joined the challenge
     */
    private void createUserTask(Challenge challenge, User user) {
        Task task = new Task();
        task.setTitle(challenge.getTitle());
        task.setDescription(challenge.getDescription());
        task.setType(challenge.getFrequency() != null ? 
                com.my.challenger.entity.enums.TaskType.valueOf(challenge.getFrequency().name()) : 
                com.my.challenger.entity.enums.TaskType.ONE_TIME);
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
        double completionPercentage = totalTasks > 0 ? ((double) completedTasks / totalTasks) * 100 : 0;
        progress.setCompletionPercentage(completionPercentage);
        
        // Update status if all tasks are completed
        if (completedTasks == totalTasks && totalTasks > 0) {
            progress.setStatus(ProgressStatus.COMPLETED);
        }
        
        progress.setUpdatedAt(LocalDateTime.now());
        challengeProgressRepository.save(progress);
    }

    /**
     * Helper method to convert Challenge entity to DTO
     */
    private ChallengeDTO convertToDTO(Challenge challenge) {
        ChallengeDTO dto = new ChallengeDTO();
        dto.setId(challenge.getId());
        dto.setTitle(challenge.getTitle());
        dto.setDescription(challenge.getDescription());
        dto.setType(challenge.getType());
        dto.setVisibility(challenge.isPublic() ?
                com.my.challenger.entity.enums.VisibilityType.PUBLIC : 
                com.my.challenger.entity.enums.VisibilityType.PRIVATE);
        dto.setStatus(challenge.getStatus());
        dto.setCreated_at(challenge.getStartDate());
        dto.setCreator_id(challenge.getCreator().getId());
        dto.setCreatorUsername(challenge.getCreator().getUsername());
        dto.setFrequency(challenge.getFrequency());
        dto.setStartDate(challenge.getStartDate());
        dto.setEndDate(challenge.getEndDate());
        
        // Get participant count
        Long participantCount = challengeProgressRepository.countByChallenge(challenge);
        dto.setParticipantCount(participantCount.intValue());
        
        // Set verification method as JSON string
        if (challenge.getVerificationMethod() != null) {
            dto.setVerificationMethod(challenge.getVerificationMethod().toString());
        }
        
        return dto;
    }
}
package com.my.challenger.service.impl.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.entity.Task;
import com.my.challenger.entity.TaskCompletion;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.challenge.LocationCoordinates;
import com.my.challenger.entity.challenge.VerificationDetails;
import com.my.challenger.entity.enums.CompletionStatus;
import com.my.challenger.entity.enums.TaskStatus;
import com.my.challenger.entity.enums.VerificationMethod;
import com.my.challenger.repository.ChallengeRepository;
import com.my.challenger.repository.TaskCompletionRepository;
import com.my.challenger.repository.TaskRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for verifying location-based challenge completions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationVerificationService {

    private final ChallengeRepository challengeRepository;
    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Verify a location submission for a challenge task
     *
     * @param challengeId The ID of the challenge
     * @param userId      The ID of the user
     * @param latitude    User's current latitude
     * @param longitude   User's current longitude
     * @param timestamp   Timestamp of the location submission
     * @return Map containing verification results
     */
    public Map<String, Object> verifyLocation(Long challengeId, Long userId, double latitude, double longitude, String timestamp) {
        try {
            log.info("Verifying location for challenge {} and user {}", challengeId, userId);

            // 1. Validate inputs and retrieve entities
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new IllegalArgumentException("Challenge not found with ID: " + challengeId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

            // Get active task for this challenge
            Task task = taskRepository.findFirstByChallengeIdAndAssignedToAndStatus(
                            challengeId, userId, TaskStatus.IN_PROGRESS)
                    .orElseThrow(() -> new IllegalArgumentException("No active task found for this challenge"));

            // 2. Check if the challenge requires location verification
            if (challenge.getVerificationMethod() != VerificationMethod.LOCATION) {
                throw new IllegalArgumentException("This challenge does not support location verification");
            }

            // 3. Parse verification details from challenge
            List<VerificationDetails> verificationDetailsList = challenge.getVerificationDetails();
            Optional<VerificationDetails> verificationDetailsOptional = verificationDetailsList.stream().filter(verificationDetails -> verificationDetails.getLocationCoordinates() != null).findAny();
            if(verificationDetailsOptional.isEmpty()){
                return new HashMap<>();
            }

            Map<String, Object> verificationDetails = parseVerificationDetails(verificationDetailsOptional.get());

            // 4. Extract required location data with fallbacks for missing values
            double targetLatitude = getDoubleValue(verificationDetails, "latitude");
            double targetLongitude = getDoubleValue(verificationDetails, "longitude");
            double requiredRadius = getDoubleValue(verificationDetails, "radius", 100.0); // Default 100 meters
            int requiredDurationSeconds = getIntValue(verificationDetails, "durationSeconds", 0); // Optional duration requirement

            // 4. Check for time requirement - if challenge requires user to be at location for a certain time
            boolean meetsTimeRequirement = true;
            String timeMessage = "";

            if (requiredDurationSeconds > 0) {
                // Find previous location check-in
                Optional<TaskCompletion> previousCheckIn = taskCompletionRepository
                        .findFirstByTaskIdAndUserIdAndStatusOrderByCompletionDateDesc(
                                task.getId(), userId, CompletionStatus.PENDING);

                if (previousCheckIn.isPresent()) {
                    LocalDateTime previousTime = previousCheckIn.get().getCompletionDate();
                    LocalDateTime currentTime = parseTimestamp(timestamp);

                    long secondsElapsed = java.time.Duration.between(previousTime, currentTime).getSeconds();

                    meetsTimeRequirement = secondsElapsed >= requiredDurationSeconds;
                    timeMessage = meetsTimeRequirement ?
                            String.format("Time requirement met (%d seconds)", secondsElapsed) :
                            String.format("Time requirement not met (need %d seconds, elapsed %d seconds)",
                                    requiredDurationSeconds, secondsElapsed);
                } else {
                    // First check-in
                    meetsTimeRequirement = false;
                    timeMessage = "Initial check-in recorded. Please remain at location for "
                            + requiredDurationSeconds + " seconds.";

                    // Save initial check-in with PENDING status
                    saveCheckIn(task.getId(), userId, latitude, longitude, parseTimestamp(timestamp),
                            0.0, CompletionStatus.PENDING, "Initial location check-in");
                }
            }

            // 5. Calculate distance between user location and target location
            double distance = calculateDistance(latitude, longitude, targetLatitude, targetLongitude);

            // 6. Check if user is within required radius
            boolean isWithinRadius = distance <= requiredRadius;

            // 7. Final verification result
            boolean isVerified = isWithinRadius && meetsTimeRequirement;

            // 8. Format result
            Map<String, Object> result = new HashMap<>();
            result.put("isVerified", isVerified);
            result.put("isWithinRadius", isWithinRadius);
            result.put("meetsTimeRequirement", meetsTimeRequirement);
            result.put("distance", distance);
            result.put("requiredRadius", requiredRadius);
            result.put("userLocation", Map.of(
                    "latitude", latitude,
                    "longitude", longitude,
                    "timestamp", timestamp
            ));
            result.put("targetLocation", Map.of(
                    "latitude", targetLatitude,
                    "longitude", targetLongitude
            ));

            StringBuilder messageBuilder = new StringBuilder();
            if (isWithinRadius) {
                messageBuilder.append(String.format("Location verified: You are %.2f meters from the target location. ", distance));
            } else {
                messageBuilder.append(String.format("Location verification failed: You are %.2f meters away from the required location. ", distance));
            }

            if (!timeMessage.isEmpty()) {
                messageBuilder.append(timeMessage);
            }

            String message = messageBuilder.toString();
            result.put("message", message);

            // 9. Save the verification result if verified or if tracking duration
            if (isVerified) {
                // Save successful verification
                saveCheckIn(task.getId(), userId, latitude, longitude, parseTimestamp(timestamp),
                        distance, CompletionStatus.VERIFIED, message);

                // Update task status if needed
                if (challenge.getType().equals("LOCATION_CHECK_IN")) {
                    task.setStatus(TaskStatus.COMPLETED);
                    taskRepository.save(task);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Error during location verification", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("isVerified", false);
            errorResult.put("message", "Error during verification: " + e.getMessage());
            errorResult.put("error", true);
            return errorResult;
        }
    }

    /**
     * Helper method to save a location check-in
     */
    private TaskCompletion saveCheckIn(Long taskId, Long userId, double latitude, double longitude,
                                       LocalDateTime completionDate, double distance, CompletionStatus status, String notes) {
        TaskCompletion completion = new TaskCompletion();
        completion.setTaskId(taskId);
        completion.setUserId(userId);
        completion.setStatus(status);
        completion.setCompletionDate(completionDate);

        if (status == CompletionStatus.VERIFIED) {
            completion.setVerificationDate(LocalDateTime.now());
        }

        completion.setVerificationProof(createLocationProof(latitude, longitude, distance, completionDate));
        completion.setNotes(notes);
        completion.setCreatedAt(LocalDateTime.now());

        return taskCompletionRepository.save(completion);
    }

    /**
     * Parse verification details from Challenge entity's VerificationDetails
     */
    private Map<String, Object> parseVerificationDetails(VerificationDetails verificationDetails) {
        try {
            if (verificationDetails == null) {
                throw new IllegalArgumentException("Verification details are missing");
            }

            Map<String, Object> details = new HashMap<>();

            // Extract location data from verificationDetails
            LocationCoordinates locationCoordinates = verificationDetails.getLocationCoordinates();
            if (locationCoordinates != null) {
                details.put("latitude", locationCoordinates.getLatitude());
                details.put("longitude", locationCoordinates.getLongitude());
            } else {
                throw new IllegalArgumentException("Location coordinates are missing in verification details");
            }

            // Get radius
            Double radius = verificationDetails.getRadius();
            if (radius != null) {
                details.put("radius", radius);
            }

            // You can add duration seconds if needed
            // details.put("durationSeconds", ...);

            return details;
        } catch (Exception e) {
            log.error("Error parsing verification details", e);
            throw new IllegalArgumentException("Invalid verification details: " + e.getMessage());
        }
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // Earth radius in meters

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // Distance in meters
    }

    /**
     * Helper method to get double value from map
     */
    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required value '" + key + "' not found in verification method");
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }

        throw new IllegalArgumentException("Value for '" + key + "' is not a number");
    }

    /**
     * Helper method to get double value from map with default value
     */
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        try {
            return getDoubleValue(map, key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Helper method to get integer value from map with default value
     */
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        try {
            Object value = map.get(key);
            if (value == null) {
                return defaultValue;
            }

            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }

            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Parse timestamp string to LocalDateTime
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Error parsing timestamp: {}. Using current time instead.", timestamp);
            return LocalDateTime.now();
        }
    }

    /**
     * Create a standardized location proof string
     */
    private String createLocationProof(double latitude, double longitude, double distance, LocalDateTime timestamp) {
        return String.format("LOCATION:%.6f,%.6f|DISTANCE:%.2f|TIME:%s",
                latitude, longitude, distance, timestamp.toString());
    }
}
//package com.my.challenger.service.impl;
//
//import com.my.challenger.entity.challenge.Challenge;
//import com.my.challenger.entity.Task;
//import com.my.challenger.entity.TaskCompletion;
//import com.my.challenger.entity.User;
//import com.my.challenger.entity.enums.CompletionStatus;
//import com.my.challenger.entity.enums.TaskStatus;
//import com.my.challenger.repository.ChallengeRepository;
//import com.my.challenger.repository.TaskCompletionRepository;
//import com.my.challenger.repository.TaskRepository;
//import com.my.challenger.repository.UserRepository;
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//@AllArgsConstructor
//@Slf4j
//public class LocationVerificationService {
//
//    private final ChallengeRepository challengeRepository;
//    private final TaskRepository taskRepository;
//    private final TaskCompletionRepository taskCompletionRepository;
//    private final UserRepository userRepository;
//
//    /**
//     * Verify a location submission for a challenge task
//     */
//    public Map<String, Object> verifyLocation(Long challengeId, Long userId, double latitude, double longitude, String timestamp) {
//        try {
//            // 1. Validate inputs
//            Challenge challenge = challengeRepository.findById(challengeId)
//                    .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
//
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
//
//            // Get active task for this challenge
//            Task task = taskRepository.findFirstByChallengeIdAndAssignedToAndStatus(
//                    challengeId, userId, TaskStatus.IN_PROGRESS)
//                    .orElseThrow(() -> new IllegalArgumentException("No active task found for this challenge"));
//
//            // 2. Parse verification details from challenge
//            Map<String, Object> verificationDetails = parseVerificationDetails(challenge.getVerificationMethod());
//
//            // 3. Extract required location data
//            double targetLatitude = getDoubleValue(verificationDetails, "latitude");
//            double targetLongitude = getDoubleValue(verificationDetails, "longitude");
//            double requiredRadius = getDoubleValue(verificationDetails, "radius", 100.0); // Default 100 meters
//
//            // 4. Calculate distance between user location and target location
//            double distance = calculateDistance(latitude, longitude, targetLatitude, targetLongitude);
//
//            // 5. Check if user is within required radius
//            boolean isWithinRadius = distance <= requiredRadius;
//
//            // 6. Format result
//            Map<String, Object> result = new HashMap<>();
//            result.put("isVerified", isWithinRadius);
//            result.put("distance", distance);
//            result.put("requiredRadius", requiredRadius);
//            result.put("userLocation", Map.of(
//                    "latitude", latitude,
//                    "longitude", longitude,
//                    "timestamp", timestamp
//            ));
//            result.put("targetLocation", Map.of(
//                    "latitude", targetLatitude,
//                    "longitude", targetLongitude
//            ));
//
//            String message = isWithinRadius
//                    ? "Location verified: You are within the required radius."
//                    : String.format("Location verification failed: You are %.2f meters away from the required location.", distance);
//            result.put("message", message);
//
//            // 7. Save the verification result if verified
//            if (isWithinRadius) {
//                LocalDateTime completionDate = parseTimestamp(timestamp);
//
//                TaskCompletion completion = new TaskCompletion();
//                completion.setTaskId(task.getId());
//                completion.setUserId(userId);
//                completion.setStatus(CompletionStatus.VERIFIED);
//                completion.setCompletionDate(completionDate);
//                completion.setVerificationDate(LocalDateTime.now());
//                completion.setVerificationProof(createLocationProof(latitude, longitude, distance, completionDate));
//                completion.setNotes(message);
//                completion.setCreatedAt(LocalDateTime.now());
//
//                taskCompletionRepository.save(completion);
//            }
//
//            return result;
//
//        } catch (Exception e) {
//            log.error("Error during location verification", e);
//            Map<String, Object> errorResult = new HashMap<>();
//            errorResult.put("isVerified", false);
//            errorResult.put("message", "Error during verification: " + e.getMessage());
//            errorResult.put("error", true);
//            return errorResult;
//        }
//    }
//
//    /**
//     * Parse verification details from challenge verification method JSON string
//     */
//    private Map<String, Object> parseVerificationDetails(String verificationMethodJson) {
//        try {
//            // This method would parse the JSON string into a map
//            // For simplicity, we'll return a dummy map in this example
//            Map<String, Object> details = new HashMap<>();
//
//            // In a real implementation, use Jackson to parse the JSON
//            // ObjectMapper mapper = new ObjectMapper();
//            // JsonNode jsonNode = mapper.readTree(verificationMethodJson);
//
//            // Placeholder values - replace with actual parsing
//            details.put("latitude", 40.7128);   // Example: New York City coordinates
//            details.put("longitude", -74.0060);
//            details.put("radius", 100.0);       // 100 meters radius
//
//            return details;
//        } catch (Exception e) {
//            log.error("Error parsing verification details", e);
//            return new HashMap<>();
//        }
//    }
//
//    /**
//     * Calculate distance between two coordinates using Haversine formula
//     */
//    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
//        final int EARTH_RADIUS = 6371000; // Earth radius in meters
//
//        double latDistance = Math.toRadians(lat2 - lat1);
//        double lonDistance = Math.toRadians(lon2 - lon1);
//
//        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
//                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
//                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
//
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//
//        return EARTH_RADIUS * c; // Distance in meters
//    }
//
//    /**
//     * Helper method to get double value from map
//     */
//    private double getDoubleValue(Map<String, Object> map, String key) {
//        Object value = map.get(key);
//        if (value == null) {
//            throw new IllegalArgumentException("Required value '" + key + "' not found");
//        }
//
//        if (value instanceof Number) {
//            return ((Number) value).doubleValue();
//        } else if (value instanceof String) {
//            return Double.parseDouble((String) value);
//        }
//
//        throw new IllegalArgumentException("Value for '" + key + "' is not a number");
//    }
//
//    /**
//     * Helper method to get double value from map with default value
//     */
//    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
//        try {
//            return getDoubleValue(map, key);
//        } catch (Exception e) {
//            return defaultValue;
//        }
//    }
//
//    /**
//     * Parse timestamp string to LocalDateTime
//     */
//    private LocalDateTime parseTimestamp(String timestamp) {
//        try {
//            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
//        } catch (Exception e) {
//            return LocalDateTime.now();
//        }
//    }
//
//    /**
//     * Create a standardized location proof string
//     */
//    private String createLocationProof(double latitude, double longitude, double distance, LocalDateTime timestamp) {
//        return String.format("LOCATION:%.6f,%.6f|DISTANCE:%.2f|TIME:%s",
//                latitude, longitude, distance, timestamp.toString());
//    }
//}
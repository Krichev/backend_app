//package com.my.challenger.service.impl.photo;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
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
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.time.LocalDateTime;
//import java.util.Base64;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@AllArgsConstructor
//@Slf4j
//public class PhotoVerificationService {
//
//    private final ChallengeRepository challengeRepository;
//    private final TaskRepository taskRepository;
//    private final TaskCompletionRepository taskCompletionRepository;
//    private final UserRepository userRepository;
//    private final ObjectMapper objectMapper;
//    private final RestTemplate restTemplate;
//
//    // DeepSeek API configuration - these should be in application.properties
//    private static final String DEEPSEEK_API_URL = "https://api.deepseek.ai/v1/images/generations";
//    private static final String DEEPSEEK_API_KEY = "your_deepseek_api_key"; // Replace with your actual API key
//
//    // Directory to store uploaded photos
//    private static final String UPLOAD_DIR = "uploads/verification-photos/";
//
//    /**
//     * Verify a photo submission for a challenge task
//     */
//    public Map<String, Object> verifyPhoto(Long challengeId, Long userId, MultipartFile photo, String prompt, String aiPrompt) {
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
//                            challengeId, userId, TaskStatus.IN_PROGRESS)
//                    .orElseThrow(() -> new IllegalArgumentException("No active task found for this challenge"));
//
//            // 2. Save the uploaded photo
//            String fileName = savePhoto(photo, challengeId, userId);
//            String photoPath = UPLOAD_DIR + fileName;
//
//            // 3. Get previous photos for this challenge to compare (for daily challenges)
//            List<TaskCompletion> previousCompletions = taskCompletionRepository
//                    .findAllByTaskIdAndUserIdOrderByCompletionDateDesc(task.getId(), userId);
//
//            String previousPhotoPath = null;
//            if (!previousCompletions.isEmpty()) {
//                previousPhotoPath = previousCompletions.get(0).getVerificationProof();
//            }
//
//            // 4. Prepare AI verification
//            if (aiPrompt == null || aiPrompt.isEmpty()) {
//                // Default AI prompt if none provided
//                aiPrompt = "Analyze if this photo shows a person wearing a shirt. " +
//                        "Check if the current shirt is different from the previous photo.";
//            }
//
//            // Add context about the challenge and what to verify
//            String fullPrompt = buildAIPrompt(aiPrompt, prompt, previousPhotoPath != null);
//
//            // 5. Send to DeepSeek API for verification
//            Map<String, Object> verificationResult = callDeepSeekAPI(photoPath, previousPhotoPath, fullPrompt);
//
//            // 6. Save the verification result
//            boolean isVerified = (boolean) verificationResult.getOrDefault("isVerified", false);
//            String message = (String) verificationResult.getOrDefault("message", "Photo verification completed");
//
//            // Create a new task completion record
//            TaskCompletion completion = new TaskCompletion();
//            completion.setTaskId(task.getId());
//            completion.setUserId(userId);
//            completion.setStatus(isVerified ? CompletionStatus.VERIFIED : CompletionStatus.REJECTED);
//            completion.setCompletionDate(LocalDateTime.now());
//            completion.setVerificationProof(photoPath);
//            completion.setNotes(message);
//            completion.setCreatedAt(LocalDateTime.now());
//
//            taskCompletionRepository.save(completion);
//
//            // 7. Return the result
//            return verificationResult;
//
//        } catch (Exception e) {
//            log.error("Error during photo verification", e);
//            Map<String, Object> errorResult = new HashMap<>();
//            errorResult.put("isVerified", false);
//            errorResult.put("message", "Error during verification: " + e.getMessage());
//            errorResult.put("error", true);
//            return errorResult;
//        }
//    }
//
//    /**
//     * Save the uploaded photo to disk
//     */
//    private String savePhoto(MultipartFile file, Long challengeId, Long userId) throws IOException {
//        // Create directory if it doesn't exist
//        File directory = new File(UPLOAD_DIR);
//        if (!directory.exists()) {
//            directory.mkdirs();
//        }
//
//        // Generate a unique filename
//        String fileName = challengeId + "_" + userId + "_" +
//                LocalDateTime.now().toString().replace(":", "-") +
//                getFileExtension(file.getOriginalFilename());
//
//        // Save the file
//        Path filePath = Paths.get(UPLOAD_DIR + fileName);
//        Files.write(filePath, file.getBytes());
//
//        return fileName;
//    }
//
//    /**
//     * Get file extension from filename
//     */
//    private String getFileExtension(String filename) {
//        if (filename == null) return ".jpg";
//        int lastDotIndex = filename.lastIndexOf(".");
//        if (lastDotIndex == -1) return ".jpg";
//        return filename.substring(lastDotIndex);
//    }
//
//    /**
//     * Build the AI prompt for verification
//     */
//    private String buildAIPrompt(String basePrompt, String challengePrompt, boolean hasPreviousPhoto) {
//        StringBuilder fullPrompt = new StringBuilder();
//
//        fullPrompt.append("You are a verification assistant for a challenge app. ");
//        fullPrompt.append("Challenge description: ").append(challengePrompt).append("\n\n");
//
//        if (hasPreviousPhoto) {
//            fullPrompt.append("There is a previous photo submission for comparison. ");
//            fullPrompt.append("Please verify that:\n");
//            fullPrompt.append("1. The current photo meets the challenge requirements\n");
//            fullPrompt.append("2. The current photo shows a different item than the previous one\n\n");
//        } else {
//            fullPrompt.append("This is the first submission. ");
//            fullPrompt.append("Please verify that the photo meets the challenge requirements.\n\n");
//        }
//
//        fullPrompt.append("Specific verification instructions: ").append(basePrompt).append("\n\n");
//        fullPrompt.append("Return a JSON response with the following fields:\n");
//        fullPrompt.append("- isVerified: boolean (true if the photo passes verification)\n");
//        fullPrompt.append("- message: string (explanation of verification result)\n");
//        fullPrompt.append("- details: object (any additional details about the verification)\n");
//
//        return fullPrompt.toString();
//    }
//
//    /**
//     * Call DeepSeek API to verify the photo
//     */
//    private Map<String, Object> callDeepSeekAPI(String photoPath, String previousPhotoPath, String prompt) {
//        try {
//            // Convert image to base64
//            byte[] photoBytes = Files.readAllBytes(Paths.get(photoPath));
//            String photoBase64 = Base64.getEncoder().encodeToString(photoBytes);
//
//            String previousPhotoBase64 = null;
//            if (previousPhotoPath != null) {
//                byte[] previousPhotoBytes = Files.readAllBytes(Paths.get(previousPhotoPath));
//                previousPhotoBase64 = Base64.getEncoder().encodeToString(previousPhotoBytes);
//            }
//
//            // Create request body
//            Map<String, Object> requestBody = new HashMap<>();
//            requestBody.put("prompt", prompt);
//            requestBody.put("image", photoBase64);
//
//            if (previousPhotoBase64 != null) {
//                requestBody.put("previous_image", previousPhotoBase64);
//            }
//
//            // Set headers
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("Authorization", "Bearer " + DEEPSEEK_API_KEY);
//
//            // Create request entity
//            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
//
//            // Call API
//            ResponseEntity<String> response = restTemplate.postForEntity(
//                    DEEPSEEK_API_URL, requestEntity, String.class);
//
//            // Parse response
//            String responseBody = response.getBody();
//            JsonNode jsonNode = objectMapper.readTree(responseBody);
//
//            // Extract verification result
//            Map<String, Object> result = new HashMap<>();
//
//            // Adapt this based on actual DeepSeek API response structure
//            if (jsonNode.has("verification_result")) {
//                JsonNode verificationResult = jsonNode.get("verification_result");
//                result.put("isVerified", verificationResult.get("is_verified").asBoolean());
//                result.put("message", verificationResult.get("message").asText());
//
//                // Add any additional details
//                if (verificationResult.has("details")) {
//                    Map<String, Object> details = objectMapper.convertValue(
//                            verificationResult.get("details"), Map.class);
//                    result.put("details", details);
//                }
//            } else {
//                // Fallback if the API doesn't return in expected format
//                result.put("isVerified", false);
//                result.put("message", "Unable to verify photo: Invalid API response");
//            }
//
//            return result;
//
//        } catch (Exception e) {
//            log.error("Error calling DeepSeek API", e);
//            Map<String, Object> errorResult = new HashMap<>();
//            errorResult.put("isVerified", false);
//            errorResult.put("message", "Error during AI verification: " + e.getMessage());
//            errorResult.put("error", true);
//            return errorResult;
//        }
//    }
//}
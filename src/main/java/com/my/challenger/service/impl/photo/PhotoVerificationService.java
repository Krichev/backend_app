package com.my.challenger.service.impl.photo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.entity.Task;
import com.my.challenger.entity.TaskCompletion;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.challenge.PhotoVerificationDetails;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.regions.Region;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoVerificationService {

    private final ChallengeRepository challengeRepository;
    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // Directory to store uploaded photos
    @Value("${app.uploads.photo-verification-dir:uploads/verification-photos/}")
    private String uploadDir;

    // AWS credentials from application properties
    @Value("${aws.access-key:}")
    private String awsAccessKey;

    @Value("${aws.secret-key:}")
    private String awsSecretKey;

    @Value("${aws.region:us-east-1}")
    private String awsRegionName;

    /**
     * Verify a photo submission for a challenge task
     */
    public Map<String, Object> verifyPhoto(Long challengeId, Long userId, MultipartFile photo, String prompt, String aiPrompt) {
        try {
            log.info("Starting photo verification for challenge {} and user {}", challengeId, userId);

            // 1. Validate inputs
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new IllegalArgumentException("Challenge not found with ID: " + challengeId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

            // Get active task for this challenge
            Task task = taskRepository.findFirstByChallengeIdAndAssignedToAndStatus(
                            challengeId, userId, TaskStatus.IN_PROGRESS)
                    .orElseThrow(() -> new IllegalArgumentException("No active task found for this challenge"));

            // 2. Save the uploaded photo
            String fileName = savePhoto(photo, challengeId, userId);
            String photoPath = uploadDir + fileName;
            log.debug("Saved photo to {}", photoPath);

            // 3. Check if the challenge requires photo verification
            if (challenge.getVerificationMethod() != VerificationMethod.PHOTO) {
                throw new IllegalArgumentException("This challenge does not support photo verification");
            }
            List<VerificationDetails> verificationDetailsList = challenge.getVerificationDetails();
            Optional<VerificationDetails> verificationDetailsOptional = verificationDetailsList.stream()
                    .filter(verificationDetails -> verificationDetails.getPhotoDetails() != null).findAny();
            if(verificationDetailsOptional.isEmpty()){
                return new HashMap<>();
            }
            // 4. Get verification details from challenge
            Map<String, Object> verificationDetails = parseVerificationDetails(verificationDetailsOptional.get());

            // 4. Get previous photos for this challenge to compare (for recurring challenges)
            String previousPhotoPath = null;
            boolean requiresPhotoComparison = Boolean.parseBoolean(
                    verificationDetails.getOrDefault("requiresPhotoComparison", "false").toString());

            if (requiresPhotoComparison) {
                log.debug("Photo comparison required. Fetching previous photos.");
                List<TaskCompletion> previousCompletions = taskCompletionRepository
                        .findAllByTaskIdAndUserIdOrderByCompletionDateDesc(task.getId(), userId);

                if (!previousCompletions.isEmpty()) {
                    previousPhotoPath = previousCompletions.get(0).getVerificationProof();
                    log.debug("Found previous photo: {}", previousPhotoPath);
                }
            }

            // 5. Prepare verification parameters
            if (prompt == null || prompt.isEmpty()) {
                // If no prompt is provided, use default from verification details
                prompt = verificationDetails.getOrDefault("description", "").toString();
            }

            // 6. Instantiate the new PhotoAnalysisService
            PhotoAnalysisService photoAnalysisService = createPhotoAnalysisService();

            // 7. Perform verification
            Map<String, Object> verificationResult;

            if (previousPhotoPath != null && requiresPhotoComparison) {
                // If this is a recurring challenge with comparison required (e.g., different shirt each day)
                log.debug("Performing photo comparison verification");
                verificationResult = photoAnalysisService.compareShirtsInPhotos(photoPath, previousPhotoPath);

                // If that succeeded, also check the content of the current photo
                if (Boolean.TRUE.equals(verificationResult.get("result"))) {
                    Map<String, Object> contentCheck = photoAnalysisService.verifyPhotoWithDescription(photoPath, prompt);

                    // Update result with combined checks
                    boolean finalResult = Boolean.TRUE.equals(contentCheck.get("result"));
                    verificationResult.put("result", finalResult);
                    verificationResult.put("message", verificationResult.get("message") +
                            (finalResult ? ". Photo content verified." : ". But photo content verification failed: " +
                                    contentCheck.get("message")));
                }
            } else {
                // For regular challenges or first occurrence of recurring challenges
                log.debug("Performing content-only verification with prompt: {}", prompt);
                verificationResult = photoAnalysisService.verifyPhotoWithDescription(photoPath, prompt);
            }

            // 8. Process verification result
            boolean isVerified = Boolean.TRUE.equals(verificationResult.get("result")) &&
                    Boolean.TRUE.equals(verificationResult.get("success"));
            String message = (String) verificationResult.getOrDefault("message", "Photo verification completed");

            // 9. Create a new task completion record
            TaskCompletion completion = new TaskCompletion();
            completion.setTaskId(task.getId());
            completion.setUserId(userId);
            completion.setStatus(isVerified ? CompletionStatus.VERIFIED : CompletionStatus.REJECTED);
            completion.setCompletionDate(LocalDateTime.now());
            completion.setVerificationProof(photoPath);
            completion.setNotes(message);
            completion.setCreatedAt(LocalDateTime.now());

            TaskCompletion savedCompletion = taskCompletionRepository.save(completion);
            log.info("Saved task completion with ID {} and status {}", savedCompletion.getId(), savedCompletion.getStatus());

            // 10. Update task status if verified
            if (isVerified) {
                task.setStatus(TaskStatus.COMPLETED);
                taskRepository.save(task);
                log.info("Updated task {} status to COMPLETED", task.getId());
            }

            // 11. Prepare response
            Map<String, Object> result = new HashMap<>(verificationResult);
            result.put("isVerified", isVerified);
            if (!result.containsKey("message")) {
                result.put("message", message);
            }
            result.put("completionId", savedCompletion.getId());
            result.put("photoPath", photoPath);

            return result;

        } catch (Exception e) {
            log.error("Error during photo verification", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("isVerified", false);
            errorResult.put("message", "Error during verification: " + e.getMessage());
            errorResult.put("error", true);
            return errorResult;
        }
    }

    /**
     * Verify a photo submission from byte array data (for base64 encoded images)
     *
     * @param challengeId The ID of the challenge
     * @param userId The ID of the user submitting the verification
     * @param imageBytes The image data as byte array
     * @param contentType The content type of the image (e.g., "image/jpeg")
     * @param prompt Optional user prompt describing the image
     * @param aiPrompt Optional AI prompt for custom verification
     * @return Map containing verification result details
     */
    public Map<String, Object> verifyPhotoFromBytes(Long challengeId, Long userId,
                                                    byte[] imageBytes, String contentType,
                                                    String prompt, String aiPrompt) {
        try {
            log.info("Starting photo verification from bytes for challenge {} and user {}", challengeId, userId);

            // 1. Validate inputs
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> new IllegalArgumentException("Challenge not found with ID: " + challengeId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

            // Get active task for this challenge
            Task task = taskRepository.findFirstByChallengeIdAndAssignedToAndStatus(
                            challengeId, userId, TaskStatus.IN_PROGRESS)
                    .orElseThrow(() -> new IllegalArgumentException("No active task found for this challenge"));

            // 2. Save the image bytes to a file
            String extension = contentTypeToExtension(contentType);
            String fileName = "ch" + challengeId + "_user" + userId + "_" +
                    LocalDateTime.now().toString().replace(":", "-").replace(".", "-") + extension;

            // Create directory if it doesn't exist
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Save the file
            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, imageBytes);
            String photoPath = uploadDir + fileName;
            log.debug("Saved photo to {}", photoPath);

            // 3. Check if the challenge requires photo verification
            if (challenge.getVerificationMethod() != VerificationMethod.PHOTO) {
                throw new IllegalArgumentException("This challenge does not support photo verification");
            }
            List<VerificationDetails> verificationDetailsList = challenge.getVerificationDetails();
            Optional<VerificationDetails> verificationDetailsOptional = verificationDetailsList.stream()
                    .filter(verificationDetails -> verificationDetails.getPhotoDetails() != null).findAny();
            if(verificationDetailsOptional.isEmpty()){
                return new HashMap<>();
            }
            // 4. Get verification details from challenge
            Map<String, Object> verificationDetails = parseVerificationDetails(verificationDetailsOptional.get());

            // 5. Get previous photos for this challenge to compare (for recurring challenges)
            String previousPhotoPath = null;
            boolean requiresPhotoComparison = Boolean.parseBoolean(
                    verificationDetails.getOrDefault("requiresPhotoComparison", "false").toString());

            if (requiresPhotoComparison) {
                log.debug("Photo comparison required. Fetching previous photos.");
                List<TaskCompletion> previousCompletions = taskCompletionRepository
                        .findAllByTaskIdAndUserIdOrderByCompletionDateDesc(task.getId(), userId);

                if (!previousCompletions.isEmpty()) {
                    previousPhotoPath = previousCompletions.get(0).getVerificationProof();
                    log.debug("Found previous photo: {}", previousPhotoPath);
                }
            }

            // 6. Prepare verification parameters
            if (prompt == null || prompt.isEmpty()) {
                // If no prompt is provided, use default from verification details
                prompt = verificationDetails.getOrDefault("description", "").toString();
            }

            // 7. Instantiate the new PhotoAnalysisService
            PhotoAnalysisService photoAnalysisService = createPhotoAnalysisService();

            // 8. Perform verification
            Map<String, Object> verificationResult;

            if (previousPhotoPath != null && requiresPhotoComparison) {
                // If this is a recurring challenge with comparison required
                log.debug("Performing photo comparison verification");
                verificationResult = photoAnalysisService.compareShirtsInPhotos(photoPath, previousPhotoPath);

                // If that succeeded, also check the content of the current photo
                if (Boolean.TRUE.equals(verificationResult.get("result"))) {
                    Map<String, Object> contentCheck = photoAnalysisService.verifyPhotoWithDescription(photoPath, prompt);

                    // Update result with combined checks
                    boolean finalResult = Boolean.TRUE.equals(contentCheck.get("result"));
                    verificationResult.put("result", finalResult);
                    verificationResult.put("message", verificationResult.get("message") +
                            (finalResult ? ". Photo content verified." : ". But photo content verification failed: " +
                                    contentCheck.get("message")));
                }
            } else {
                // For regular challenges or first occurrence of recurring challenges
                log.debug("Performing content-only verification with prompt: {}", prompt);
                verificationResult = photoAnalysisService.verifyPhotoWithDescription(photoPath, prompt);
            }

            // 9. Process verification result
            boolean isVerified = Boolean.TRUE.equals(verificationResult.get("result")) &&
                    Boolean.TRUE.equals(verificationResult.get("success"));
            String message = (String) verificationResult.getOrDefault("message", "Photo verification completed");

            // 10. Create a new task completion record
            TaskCompletion completion = new TaskCompletion();
            completion.setTaskId(task.getId());
            completion.setUserId(userId);
            completion.setStatus(isVerified ? CompletionStatus.VERIFIED : CompletionStatus.REJECTED);
            completion.setCompletionDate(LocalDateTime.now());
            completion.setVerificationProof(photoPath);
            completion.setNotes(message);
            completion.setCreatedAt(LocalDateTime.now());

            TaskCompletion savedCompletion = taskCompletionRepository.save(completion);
            log.info("Saved task completion with ID {} and status {}", savedCompletion.getId(), savedCompletion.getStatus());

            // 11. Update task status if verified
            if (isVerified) {
                task.setStatus(TaskStatus.COMPLETED);
                taskRepository.save(task);
                log.info("Updated task {} status to COMPLETED", task.getId());
            }

            // 12. Prepare response
            Map<String, Object> result = new HashMap<>(verificationResult);
            result.put("isVerified", isVerified);
            if (!result.containsKey("message")) {
                result.put("message", message);
            }
            result.put("completionId", savedCompletion.getId());
            result.put("photoPath", photoPath);

            return result;

        } catch (Exception e) {
            log.error("Error during photo verification from bytes", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("isVerified", false);
            errorResult.put("message", "Error during verification: " + e.getMessage());
            errorResult.put("error", true);
            return errorResult;
        }
    }

    /**
     * Convert content type to file extension
     */
    private String contentTypeToExtension(String contentType) {
        if (contentType == null) {
            return ".jpg";
        }

        switch (contentType.toLowerCase()) {
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/bmp":
                return ".bmp";
            case "image/webp":
                return ".webp";
            case "image/jpeg":
            case "image/jpg":
            default:
                return ".jpg";
        }
    }

    /**
     * Create an instance of PhotoAnalysisService with AWS credentials
     */
    private PhotoAnalysisService createPhotoAnalysisService() {
        Region awsRegion = Region.of(awsRegionName);
        return new PhotoAnalysisService(awsAccessKey, awsSecretKey, awsRegion);
    }

    /**
     * Parse verification details from Challenge entity's VerificationDetails
     * Extracts only the photo-specific information
     */
    private Map<String, Object> parseVerificationDetails(VerificationDetails verificationDetails) {
        try {
            Map<String, Object> details = new HashMap<>();

            if (verificationDetails == null) {
                // Return default values if no details provided
                details.put("description", "");
                details.put("requiresPhotoComparison", false);
                details.put("verificationMode", "standard");
                return details;
            }

            // Extract photo verification details
            PhotoVerificationDetails photoDetails = verificationDetails.getPhotoDetails();
            if (photoDetails != null) {
                // Get description
                if (photoDetails.getDescription() != null) {
                    details.put("description", photoDetails.getDescription());
                } else {
                    details.put("description", "");
                }

                // Get photo comparison requirement
                if (photoDetails.getRequiresPhotoComparison() != null) {
                    details.put("requiresPhotoComparison", photoDetails.getRequiresPhotoComparison());
                } else {
                    details.put("requiresPhotoComparison", false);
                }

                // Get verification mode
                if (photoDetails.getVerificationMode() != null) {
                    details.put("verificationMode", photoDetails.getVerificationMode());
                } else {
                    details.put("verificationMode", "standard");
                }
            } else {
                // Fallback: try to use activityType as description if photoDetails not available
                if (verificationDetails.getActivityType() != null) {
                    details.put("description", verificationDetails.getActivityType());
                } else {
                    details.put("description", "");
                }

                // Default values
                details.put("requiresPhotoComparison", false);
                details.put("verificationMode", "standard");
            }

            return details;
        } catch (Exception e) {
            log.error("Error parsing verification details", e);
            // Return default values on error
            Map<String, Object> defaultDetails = new HashMap<>();
            defaultDetails.put("description", "");
            defaultDetails.put("requiresPhotoComparison", false);
            defaultDetails.put("verificationMode", "standard");
            return defaultDetails;
        }
    }

    /**
     * Save the uploaded photo to disk
     */
    private String savePhoto(MultipartFile file, Long challengeId, Long userId) throws IOException {
        // Create directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate a unique filename
        String fileName = "ch" + challengeId + "_user" + userId + "_" +
                LocalDateTime.now().toString().replace(":", "-").replace(".", "-") +
                getFileExtension(file.getOriginalFilename());

        // Save the file
        Path filePath = Paths.get(uploadDir + fileName);
        Files.write(filePath, file.getBytes());

        return fileName;
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null) return ".jpg";
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) return ".jpg";
        return filename.substring(lastDotIndex);
    }
}
package com.my.challenger.util;

import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.StorageEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Utility for generating standardized S3 object keys with hierarchical structure.
 * Schema: {env}/{hashPrefix}/{ownerType}/{ownerId}/{quizId}/{questionId}/{mediaType}/{uuid}.{ext}
 */
@Component
public class S3KeyGenerator {

    /**
     * Generates a hierarchical S3 key.
     *
     * @param env        Environment (dev/staging/prod)
     * @param ownerId    ID of the resource owner (usually User ID)
     * @param ownerType  Type of owner (default: "user")
     * @param quizId     ID of the quiz (nullable, defaults to "unassigned")
     * @param questionId ID of the question (nullable, defaults to "temp")
     * @param mediaType  Type of media (image/video/audio)
     * @param extension  File extension (e.g., "jpg", ".png")
     * @return Full S3 key path
     */
    public String generateKey(StorageEnvironment env, Long ownerId, String ownerType,
                              Long quizId, Long questionId, MediaType mediaType, String extension) {
        
        if (env == null || ownerId == null || mediaType == null) {
            throw new IllegalArgumentException("Environment, ownerId, and mediaType are required");
        }

        String hashPrefix = computeHashPrefix(ownerId);
        String safeOwnerType = StringUtils.hasText(ownerType) ? ownerType : "user";
        String safeQuizId = (quizId != null) ? "quiz_" + quizId : "unassigned";
        String safeQuestionId = (questionId != null) ? "q_" + questionId : "temp";
        String uuid = UUID.randomUUID().toString();
        
        // Ensure extension has dot if provided
        String safeExtension = "";
        if (StringUtils.hasText(extension)) {
            safeExtension = extension.startsWith(".") ? extension : "." + extension;
        }

        return String.format("%s/%s/%s/%s/%s/%s/%s/%s%s",
                env.getPathValue(),
                hashPrefix,
                safeOwnerType,
                ownerId,
                safeQuizId,
                safeQuestionId,
                mediaType.name().toLowerCase(),
                uuid,
                safeExtension.toLowerCase());
    }

    /**
     * Computes a 2-character hex hash prefix from the ownerId to prevent S3 hot-spotting.
     * Logic: hash(ownerId) % 256 -> hex string (00-ff)
     * 
     * @param ownerId The numeric ID to hash
     * @return Two-character hexadecimal string
     */
    public String computeHashPrefix(Long ownerId) {
        if (ownerId == null) {
            return "00";
        }
        // Use hashCode() of the Long object to get a consistent integer hash
        int hash = Math.abs(ownerId.hashCode());
        int mod = hash % 256;
        return String.format("%02x", mod);
    }
}

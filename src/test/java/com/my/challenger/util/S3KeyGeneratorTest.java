package com.my.challenger.util;

import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.StorageEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class S3KeyGeneratorTest {

    private final S3KeyGenerator generator = new S3KeyGenerator();

    @Test
    void generateKey_ShouldReturnCorrectFormat() {
        String key = generator.generateKey(
                StorageEnvironment.DEV,
                123L,
                "user",
                456L,
                789L,
                MediaType.IMAGE,
                "jpg"
        );

        // Format: dev/{hash}/user/123/quiz_456/q_789/image/{uuid}.jpg
        assertNotNull(key);
        assertTrue(key.startsWith("dev/"));
        assertTrue(key.contains("/user/123/"));
        assertTrue(key.contains("/quiz_456/"));
        assertTrue(key.contains("/q_789/"));
        assertTrue(key.contains("/image/"));
        assertTrue(key.endsWith(".jpg"));
        
        // Verify hash prefix length (2 chars)
        String[] parts = key.split("/");
        assertEquals(2, parts[1].length());
    }

    @ParameterizedTest
    @CsvSource({
            "1, 01",
            "2, 02",
            "255, ff",
            "256, 00",
            "12345, 39"
    })
    void computeHashPrefix_ShouldReturnCorrectHex(Long input, String expected) {
        assertEquals(expected, generator.computeHashPrefix(input));
    }

    @Test
    void generateKey_WithNullOptionals_ShouldUseDefaults() {
        String key = generator.generateKey(
                StorageEnvironment.PROD,
                1L,
                null, // ownerType -> "user"
                null, // quizId -> "unassigned"
                null, // questionId -> "temp"
                MediaType.AUDIO,
                ".mp3"
        );

        assertTrue(key.startsWith("prod/"));
        assertTrue(key.contains("/user/1/"));
        assertTrue(key.contains("/unassigned/"));
        assertTrue(key.contains("/temp/"));
        assertTrue(key.contains("/audio/"));
    }
    
    @Test
    void generateKey_ShouldThrowOnNullRequired() {
        assertThrows(IllegalArgumentException.class, () -> generator.generateKey(
                null, 1L, "user", 1L, 1L, MediaType.IMAGE, "png"));
                
        assertThrows(IllegalArgumentException.class, () -> generator.generateKey(
                StorageEnvironment.DEV, null, "user", 1L, 1L, MediaType.IMAGE, "png"));
                
        assertThrows(IllegalArgumentException.class, () -> generator.generateKey(
                StorageEnvironment.DEV, 1L, "user", 1L, 1L, null, "png"));
    }
}

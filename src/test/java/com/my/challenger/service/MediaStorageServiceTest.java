package com.my.challenger.service;

import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.repository.MediaFileRepository;
import com.my.challenger.service.impl.MinioMediaStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private MediaFileRepository mediaFileRepository;

    @InjectMocks
    private MinioMediaStorageService mediaStorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaStorageService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(mediaStorageService, "minioEndpoint", "http://localhost:9000");
    }

    @Test
    void testStoreMedia_GeneratesUniqueStorageKey() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.pdf",
                "application/pdf",
                "test content".getBytes()
        );
        Long userId = 100L;
        Long entityId = 500L;

        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile mf = invocation.getArgument(0);
            mf.setId(1L);
            // Simulate @PrePersist
            if (mf.getStorageKey() == null) {
                ReflectionTestUtils.setField(mf, "storageKey", UUID.randomUUID());
            }
            return mf;
        });

        // Act
        MediaFile result = mediaStorageService.storeMedia(file, entityId, MediaCategory.QUIZ_QUESTION, userId);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getStorageKey());
        assertEquals("test-file.pdf", result.getOriginalFilename());
        assertNotEquals("test-file.pdf", result.getFilename(), "Stored filename should be UUID-based, not original");
        
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(mediaFileRepository).save(any(MediaFile.class));
    }

    @Test
    void testStoreMedia_SameFilenameDifferentUsers_ShouldNotConflict() {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile("file", "report.pdf", "application/pdf", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "report.pdf", "application/pdf", "content2".getBytes());
        
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile mf = invocation.getArgument(0);
            mf.setId(1L);
            if (mf.getStorageKey() == null) {
                ReflectionTestUtils.setField(mf, "storageKey", UUID.randomUUID());
            }
            return mf;
        });

        // Act
        MediaFile result1 = mediaStorageService.storeMedia(file1, 1L, MediaCategory.TEMPORARY, 101L);
        MediaFile result2 = mediaStorageService.storeMedia(file2, 2L, MediaCategory.TEMPORARY, 102L);

        // Assert
        assertNotEquals(result1.getS3Key(), result2.getS3Key());
        assertNotEquals(result1.getFilename(), result2.getFilename());
        assertEquals("report.pdf", result1.getOriginalFilename());
        assertEquals("report.pdf", result2.getOriginalFilename());
    }

    @Test
    void testStoreMedia_CalculatesContentHash() {
        // Arrange
        byte[] content = "unique content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", content);
        
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> {
            MediaFile mf = invocation.getArgument(0);
            return mf;
        });

        // Act
        MediaFile result = mediaStorageService.storeMedia(file, null, MediaCategory.TEMPORARY, 1L);

        // Assert
        assertNotNull(result.getContentHash());
        // Verify hash length (SHA-256 is 64 hex chars)
        assertEquals(64, result.getContentHash().length());
    }

    @Test
    void testStoreMedia_DuplicateContentDetection() {
        // Arrange
        byte[] content = "duplicate content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "file1.txt", "text/plain", content);
        Long userId = 1L;
        
        MediaFile existingFile = new MediaFile();
        existingFile.setId(99L);
        existingFile.setOriginalFilename("existing.txt");
        
        // Mock repository to return existing file for same hash and user
        when(mediaFileRepository.findByContentHashAndUploadedBy(anyString(), eq(userId)))
                .thenReturn(List.of(existingFile));

        // Act
        MediaFile result = mediaStorageService.storeMedia(file, null, MediaCategory.TEMPORARY, userId);

        // Assert
        assertEquals(99L, result.getId());
        assertEquals("existing.txt", result.getOriginalFilename());
        // Should NOT upload to S3 if duplicate found
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
package com.my.challenger.service.integration;

import com.my.challenger.config.StorageProperties;
import com.my.challenger.service.impl.MinioMediaStorageService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class KaraokeScoringClient {

    private final RestTemplate restTemplate;
    private final MinioMediaStorageService mediaStorageService;
    private final StorageProperties storageProperties;

    @Value("${challenger.karaoke.base-url:http://localhost:8083}")
    private String karaokeBaseUrl;

    @Value("${challenger.karaoke.scoring-endpoint:/api/scoring/analyze}")
    private String scoringEndpoint;

    /**
     * Score audio recording against reference using presigned URLs
     */
    public ScoringResult scoreAudio(
            String userAudioS3Key,
            String userAudioBucket,
            String referenceAudioS3Key,
            String referenceAudioBucket,
            String challengeType,
            Integer rhythmBpm,
            String timeSignature) {

        log.info("🎵 Preparing scoring request for type={}", challengeType);

        try {
            // Generate presigned URLs
            String userAudioUrl = mediaStorageService.generatePresignedUrl(userAudioBucket, userAudioS3Key);

            if (userAudioUrl == null) {
                log.error("❌ Failed to generate presigned URL for user audio: bucket={}, key={}",
                        userAudioBucket, userAudioS3Key);
                return zeroScoreResult("Failed to generate presigned URL for user audio");
            }

            String referenceAudioUrl = null;
            if (referenceAudioS3Key != null) {
                referenceAudioUrl = mediaStorageService.generatePresignedUrl(referenceAudioBucket, referenceAudioS3Key);

                if (referenceAudioUrl == null) {
                    log.error("❌ Failed to generate presigned URL for reference audio: bucket={}, key={}",
                            referenceAudioBucket, referenceAudioS3Key);
                }
            }

            log.info("🔗 Generated presigned URLs for scoring: hasUserUrl={}, hasRefUrl={}",
                    userAudioUrl != null, referenceAudioUrl != null);

            ScoringRequest request = ScoringRequest.builder()
                    .userAudioUrl(userAudioUrl)
                    .referenceAudioUrl(referenceAudioUrl)
                    .challengeType(challengeType)
                    .rhythmBpm(rhythmBpm)
                    .timeSignature(timeSignature)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ScoringRequest> entity = new HttpEntity<>(request, headers);

            String url = karaokeBaseUrl + scoringEndpoint;
            log.info("🚀 Calling Karaoke service at: {}", url);

            ResponseEntity<ScoringResult> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ScoringResult.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("✅ Scoring complete: overall={}", response.getBody().getOverallScore());
                return response.getBody();
            }

            throw new RuntimeException("Karaoke service returned unexpected response: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("❌ Error calling Karaoke service: {}", e.getMessage(), e);
            return zeroScoreResult(e.getMessage());
        }
    }

    private ScoringResult zeroScoreResult(String errorMessage) {
        return ScoringResult.builder()
                .pitchScore(0.0)
                .rhythmScore(0.0)
                .voiceScore(0.0)
                .overallScore(0.0)
                .detailedMetrics("{\"error\": \"" + errorMessage + "\"}")
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringRequest {
        private String userAudioUrl;
        private String referenceAudioUrl;
        private String challengeType;
        private Integer rhythmBpm;
        private String timeSignature;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringResult {
        private Double pitchScore;
        private Double rhythmScore;
        private Double voiceScore;
        private Double overallScore;
        private String detailedMetrics;
    }
}

package com.my.challenger.service.integration;

import com.my.challenger.entity.enums.AudioChallengeType;
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
public class KaraokeServiceClient {

    private final RestTemplate restTemplate;

    @Value("${karaoke.service.url:http://localhost:8081}")
    private String karaokeServiceUrl;

    /**
     * Score audio recording against reference
     */
    public ScoringResult scoreAudio(
            String userAudioPath,
            String referenceAudioPath,
            AudioChallengeType challengeType,
            Integer rhythmBpm,
            String timeSignature) {

        log.info("🎵 Calling Karaoke service for scoring: type={}", challengeType);

        try {
            ScoringRequest request = ScoringRequest.builder()
                    .userAudioPath(userAudioPath)
                    .referenceAudioPath(referenceAudioPath)
                    .challengeType(challengeType.name())
                    .rhythmBpm(rhythmBpm)
                    .timeSignature(timeSignature)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ScoringRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ScoringResult> response = restTemplate.exchange(
                    karaokeServiceUrl + "/api/scoring/analyze",
                    HttpMethod.POST,
                    entity,
                    ScoringResult.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("✅ Scoring complete: overall={}", response.getBody().getOverallScore());
                return response.getBody();
            }

            throw new RuntimeException("Karaoke service returned unexpected response");

        } catch (Exception e) {
            log.error("❌ Error calling Karaoke service: {}", e.getMessage(), e);
            // Return zero scores on failure
            return ScoringResult.builder()
                    .pitchScore(0.0)
                    .rhythmScore(0.0)
                    .voiceScore(0.0)
                    .overallScore(0.0)
                    .detailedMetrics("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringRequest {
        private String userAudioPath;
        private String referenceAudioPath;
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

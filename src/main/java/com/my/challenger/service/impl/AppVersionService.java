package com.my.challenger.service.impl;

import com.my.challenger.dto.appversion.AppVersionCheckResponse;
import com.my.challenger.dto.appversion.GitHubReleaseDTO;
import com.my.challenger.dto.appversion.UpdateAppVersionConfigRequest;
import com.my.challenger.entity.AppVersionConfig;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.AppVersionConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppVersionService {

    private final AppVersionConfigRepository configRepository;
    private final RestTemplate restTemplate;

    // Simple in-memory cache
    private volatile GitHubReleaseDTO cachedRelease;
    private volatile Instant cacheExpiry = Instant.MIN;

    public AppVersionCheckResponse checkForUpdate(String platform, String currentVersion) {
        AppVersionConfig config = configRepository.findByPlatform(platform)
                .orElseThrow(() -> new ResourceNotFoundException("AppVersionConfig not found for platform: " + platform));

        if (!config.getEnabled()) {
            return AppVersionCheckResponse.builder()
                    .updateAvailable(false)
                    .build();
        }

        GitHubReleaseDTO latestRelease = fetchLatestRelease(config);
        if (latestRelease == null) {
            log.warn("Could not fetch latest release from GitHub for platform: {}", platform);
            return AppVersionCheckResponse.builder()
                    .updateAvailable(false)
                    .build();
        }

        String latestTagName = latestRelease.getTagName();
        String latestVersion = latestTagName.startsWith("v") ? latestTagName.substring(1) : latestTagName;

        boolean updateAvailable = compareVersions(currentVersion, latestVersion) < 0;
        boolean forceUpdate = compareVersions(currentVersion, config.getMinSupportedVersion()) < 0;
        
        // Additional force update check if config.forceUpdateBelow is set
        if (config.getForceUpdateBelow() != null && !config.getForceUpdateBelow().isEmpty()) {
            if (compareVersions(currentVersion, config.getForceUpdateBelow()) < 0) {
                forceUpdate = true;
            }
        }

        GitHubReleaseDTO.GitHubAsset apkAsset = latestRelease.getAssets() != null 
                ? latestRelease.getAssets().stream()
                    .filter(asset -> asset.getName().endsWith(".apk"))
                    .findFirst()
                    .orElse(null)
                : null;

        Integer latestVersionCode = extractVersionCode(latestVersion);

        return AppVersionCheckResponse.builder()
                .latestVersion(latestVersion)
                .latestVersionCode(latestVersionCode)
                .downloadUrl(apkAsset != null ? apkAsset.getBrowserDownloadUrl() : null)
                .releaseNotes(latestRelease.getBody())
                .releaseDate(Instant.parse(latestRelease.getPublishedAt()))
                .forceUpdate(forceUpdate)
                .minSupportedVersion(config.getMinSupportedVersion())
                .fileSizeBytes(apkAsset != null ? apkAsset.getSize() : null)
                .updateAvailable(updateAvailable)
                .build();
    }

    private synchronized GitHubReleaseDTO fetchLatestRelease(AppVersionConfig config) {
        if (cachedRelease != null && Instant.now().isBefore(cacheExpiry)) {
            return cachedRelease;
        }

        String url = String.format("https://api.github.com/repos/%s/%s/releases/latest", 
                config.getGithubOwner(), config.getGithubRepo());

        try {
            log.info("Fetching latest release from GitHub: {}", url);
            ResponseEntity<GitHubReleaseDTO> response = restTemplate.getForEntity(url, GitHubReleaseDTO.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cachedRelease = response.getBody();
                cacheExpiry = Instant.now().plus(config.getCacheTtlMinutes(), ChronoUnit.MINUTES);
                return cachedRelease;
            }
        } catch (Exception e) {
            log.error("Failed to fetch latest release from GitHub", e);
            // If we have an expired cache, return it as fallback
            if (cachedRelease != null) {
                log.info("Returning expired cached release as fallback");
                return cachedRelease;
            }
        }
        return null;
    }

    /**
     * Compare version strings: "1.0.0.153" vs "1.0.0.160"
     * Returns:
     * - negative if a < b (update available if a is current)
     * - 0 if a == b
     * - positive if a > b
     */
    public static int compareVersions(String a, String b) {
        if (a == null || b == null) return 0;
        
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int length = Math.max(partsA.length, partsB.length);
        
        for (int i = 0; i < length; i++) {
            int valA = i < partsA.length ? Integer.parseInt(partsA[i].replaceAll("[^0-9]", "")) : 0;
            int valB = i < partsB.length ? Integer.parseInt(partsB[i].replaceAll("[^0-9]", "")) : 0;
            if (valA < valB) return -1;
            if (valA > valB) return 1;
        }
        return 0;
    }

    private Integer extractVersionCode(String version) {
        try {
            String[] parts = version.split("\\.");
            if (parts.length > 0) {
                return Integer.parseInt(parts[parts.length - 1].replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            log.warn("Could not extract version code from version: {}", version);
        }
        return null;
    }

    @Transactional
    public AppVersionConfig updateConfig(String platform, UpdateAppVersionConfigRequest request) {
        AppVersionConfig config = configRepository.findByPlatform(platform)
                .orElseThrow(() -> new ResourceNotFoundException("AppVersionConfig not found for platform: " + platform));

        if (request.getMinSupportedVersion() != null) config.setMinSupportedVersion(request.getMinSupportedVersion());
        if (request.getForceUpdateBelow() != null) config.setForceUpdateBelow(request.getForceUpdateBelow());
        if (request.getCacheTtlMinutes() != null) config.setCacheTtlMinutes(request.getCacheTtlMinutes());
        if (request.getEnabled() != null) config.setEnabled(request.getEnabled());

        return configRepository.save(config);
    }
}

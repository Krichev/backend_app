package com.my.challenger.dto.appversion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVersionCheckResponse {
    private String latestVersion;
    private Integer latestVersionCode;
    private String downloadUrl;
    private String releaseNotes;
    private Instant releaseDate;
    private boolean forceUpdate;
    private String minSupportedVersion;
    private Long fileSizeBytes;
    private boolean updateAvailable;
}

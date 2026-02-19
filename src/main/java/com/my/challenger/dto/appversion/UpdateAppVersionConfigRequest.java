package com.my.challenger.dto.appversion;

import lombok.Data;

@Data
public class UpdateAppVersionConfigRequest {
    private String minSupportedVersion;
    private String forceUpdateBelow;
    private Integer cacheTtlMinutes;
    private Boolean enabled;
}

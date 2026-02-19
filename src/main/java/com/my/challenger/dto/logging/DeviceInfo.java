package com.my.challenger.dto.logging;

import lombok.Data;

@Data
public class DeviceInfo {
    private String platform;     // "android" | "ios"
    private String appVersion;
    private String osVersion;
    private String deviceModel;
}

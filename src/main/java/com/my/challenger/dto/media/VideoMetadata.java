package com.my.challenger.dto.media;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoMetadata {
    private int width;
    private int height;
    private double durationSeconds;
    private long bitrate;
    private double frameRate;
    private String codec;
    private String pixelFormat;
}
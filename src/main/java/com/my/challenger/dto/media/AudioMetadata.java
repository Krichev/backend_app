package com.my.challenger.dto.media;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AudioMetadata {
    private double durationSeconds;
    private long bitrate;
    private int sampleRate;
    private int channels;
    private String codec;
}
package com.my.challenger.dto.media;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoConversionOptions {
    @Builder.Default
    private String format = "mp4";
    private Integer width;
    private Integer height;
    private Long bitrate; // in bits per second
    private Double frameRate;
    @Builder.Default
    private String videoCodec = "libx264";
    /**
     * Corresponds to FFmpeg's -preset option.
     * Examples: ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow.
     */
    @Builder.Default
    private String qualityPreset = "medium";
}
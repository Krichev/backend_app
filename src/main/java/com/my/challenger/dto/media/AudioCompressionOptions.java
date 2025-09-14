package com.my.challenger.dto.media;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AudioCompressionOptions {
    @Builder.Default
    private String format = "mp3";
    @Builder.Default
    private Long bitrate = 128000L; // in bits per second (128 kbps)
    @Builder.Default
    private Integer sampleRate = 44100; // Hz

    private Integer channels;
    /**
     * Recommended codec for mp3 is 'libmp3lame'. For aac, use 'aac'.
     */
    @Builder.Default
    private String audioCodec = "libmp3lame";
}
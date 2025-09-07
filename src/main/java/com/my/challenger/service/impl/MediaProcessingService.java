package com.my.challenger.service.impl;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.*;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.my.challenger.dto.media.AudioCompressionOptions;
import com.my.challenger.dto.media.AudioMetadata;
import com.my.challenger.dto.media.VideoConversionOptions;
import com.my.challenger.dto.media.VideoMetadata;
import com.my.challenger.exception.MediaProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MediaProcessingService {

    private final Path ffmpegPath;
    private final Path ffprobePath;

    // FIX: Use constructor injection for dependencies. It improves testability and ensures immutability.
    public MediaProcessingService(@Value("${app.ffmpeg.path:ffmpeg}") String ffmpegPath,
                                  @Value("${app.ffprobe.path:ffprobe}") String ffprobePath) {
        this.ffmpegPath = Paths.get(ffmpegPath);
        this.ffprobePath = Paths.get(ffprobePath);
    }

    /**
     * Extracts comprehensive metadata from a video file.
     *
     * @param videoPath Path to the video file.
     * @return A VideoMetadata object.
     * @throws MediaProcessingException if metadata extraction fails.
     */
    public VideoMetadata extractVideoMetadata(String videoPath) {
        Objects.requireNonNull(videoPath, "videoPath must not be null");
        try {
            FFprobeResult result = getFfprobeResult(videoPath);

            Stream videoStream = result.getStreams().stream()
                    .filter(stream -> StreamType.VIDEO.equals(stream.getCodecType()))
                    .findFirst()
                    .orElseThrow(() -> new MediaProcessingException("No video stream found in: " + videoPath));

            Format format = result.getFormat();

            // FIX: Populate all fields in the metadata object.
            return VideoMetadata.builder()
                    .width(videoStream.getWidth())
                    .height(videoStream.getHeight())
                    .durationSeconds(format.getDuration())
                    .bitrate(format.getBitRate() != null ? format.getBitRate() : 0L)
                    .frameRate(parseFrameRate(videoStream.getRFrameRate()))
                    .codec(videoStream.getCodecName())
                    .pixelFormat(videoStream.getPixelFormat())
                    .build();

        } catch (Exception e) {
            log.error("Failed to extract video metadata from: {}", videoPath, e);
            // FIX: Use a specific, custom exception.
            throw new MediaProcessingException("Video metadata extraction failed for: " + videoPath, e);
        }
    }

    /**
     * Extracts comprehensive metadata from an audio file.
     *
     * @param audioPath Path to the audio file.
     * @return An AudioMetadata object.
     * @throws MediaProcessingException if metadata extraction fails.
     */
    public AudioMetadata extractAudioMetadata(String audioPath) {
        Objects.requireNonNull(audioPath, "audioPath must not be null");
        try {
            FFprobeResult result = getFfprobeResult(audioPath);

            Stream audioStream = result.getStreams().stream()
                    .filter(stream -> StreamType.AUDIO.equals(stream.getCodecType()))
                    .findFirst()
                    .orElseThrow(() -> new MediaProcessingException("No audio stream found in: " + audioPath));

            Format format = result.getFormat();

            // FIX: Populate all fields in the metadata object.
            return AudioMetadata.builder()
                    .durationSeconds(format.getDuration())
                    .bitrate(audioStream.getBitRate() != null ? audioStream.getBitRate() : 0L)
                    .sampleRate(audioStream.getSampleRate())
                    .channels(audioStream.getChannels())
                    .codec(audioStream.getCodecName())
                    .build();

        } catch (Exception e) {
            log.error("Failed to extract audio metadata from: {}", audioPath, e);
            throw new MediaProcessingException("Audio metadata extraction failed for: " + audioPath, e);
        }
    }

    /**
     * Generates a single thumbnail from a video.
     *
     * @param videoPath  Path to the video file.
     * @param outputDir  Directory to save the thumbnail in.
     * @param seekTime   The time from which to grab the thumbnail.
     * @return The full path to the generated thumbnail.
     * @throws MediaProcessingException if thumbnail generation fails.
     */
    public String generateVideoThumbnail(String videoPath, Path outputDir, Duration seekTime) {
        Objects.requireNonNull(videoPath, "videoPath must not be null");
        Objects.requireNonNull(outputDir, "outputDir must not be null");
        Objects.requireNonNull(seekTime, "seekTime must not be null");

        // FIX: Use UUID for filenames to prevent collisions.
        String thumbnailFilename = "thumb_" + UUID.randomUUID() + ".jpg";
        Path thumbnailPath = outputDir.resolve(thumbnailFilename);

        try {
            FFmpeg.atPath(ffmpegPath)
                    .addInput(UrlInput.fromPath(Paths.get(videoPath))
                            // FIX: Use setPosition to seek to a specific time for the thumbnail.
                            .setPosition(seekTime.toMillis(), TimeUnit.MILLISECONDS))
                    // FIX: Simplified progress listener. The old calculation was arbitrary and misleading.
                    .addOutput(UrlOutput.toPath(thumbnailPath)
                            .disableStream(StreamType.VIDEO) // <-- Correct replacement
                            .setAudioCodec(options.getAudioCodec())
                            .setAudioBitRate(options.getBitrate())
                            .setAudioSampleRate(options.getSampleRate())
                            .setOverwriteOutput(true))
                    // FIX: Simplified progress listener. The old calculation was arbitrary and misleading.
                    .setProgressListener(progress -> log.debug("Thumbnail generation progress: {}ms", progress.getTimeMillis()))
                    .execute();

            log.info("Successfully generated thumbnail: {}", thumbnailPath);
            return thumbnailPath.toString();

        } catch (Exception e) {
            log.error("Failed to generate video thumbnail from: {}", videoPath, e);
            throw new MediaProcessingException("Thumbnail generation failed for: " + videoPath, e);
        }
    }

    /**
     * Asynchronously generates a single thumbnail from a video, taking the frame from the 5-second mark.
     *
     * @param videoPath Path to the video file.
     * @param outputDir Directory to save the thumbnail in.
     * @return A CompletableFuture containing the path to the thumbnail.
     */
    @Async
    // NOTE: Ensure you have a configured TaskExecutor for @Async to work optimally.
    public CompletableFuture<String> generateVideoThumbnailAsync(String videoPath, Path outputDir) {
        return CompletableFuture.supplyAsync(() -> generateVideoThumbnail(videoPath, outputDir, Duration.ofSeconds(5)));
    }

    /**
     * Converts a video file according to the specified options.
     *
     * @param inputPath Path to the input video.
     * @param outputDir Directory for the output file.
     * @param options   Conversion options.
     * @return The full path to the converted video.
     * @throws MediaProcessingException if conversion fails.
     */
    public String convertVideo(String inputPath, Path outputDir, VideoConversionOptions options) {
        Objects.requireNonNull(inputPath, "inputPath must not be null");
        Objects.requireNonNull(outputDir, "outputDir must not be null");
        Objects.requireNonNull(options, "options must not be null");

        String outputFilename = "converted_" + UUID.randomUUID() + "." + options.getFormat();
        Path outputPath = outputDir.resolve(outputFilename);

        try {
            // Create a configurable UrlOutput object
            UrlOutput output = UrlOutput.toPath(outputPath)
                    .setOverwriteOutput(true)
                    .setFormat(options.getFormat());

            // Apply options to the UrlOutput object
            if (options.getWidth() != null && options.getHeight() != null) {
                output.setVideoFilter("scale=" + options.getWidth() + ":" + options.getHeight());
            }
            if (options.getBitrate() != null) {
                output.setVideoBitRate(options.getBitrate());
            }
            if (options.getFrameRate() != null) {
                output.setFrameRate(options.getFrameRate());
            }
            if (options.getVideoCodec() != null) {
                // FIX: Use the new generic setCodec method
                output.setCodec(StreamType.VIDEO, options.getVideoCodec());
            }

            FFmpeg.atPath(ffmpegPath)
                    .addInput(UrlInput.fromPath(Paths.get(inputPath)))
                    // Pass the fully configured output object
                    .addOutput(output)
                    .addArgument("-preset")
                    .addArgument(options.getQualityPreset())
                    .setProgressListener(progress -> log.debug("Video conversion progress: frame {}", progress.getFrame()))
                    .execute();

            log.info("Video conversion completed: {} -> {}", inputPath, outputPath);
            return outputPath.toString();

        } catch (Exception e) {
            log.error("Failed to convert video: {}", inputPath, e);
            throw new MediaProcessingException("Video conversion failed for: " + inputPath, e);
        }
    }

    @Async
    public CompletableFuture<String> convertVideoAsync(String inputPath, Path outputDir, VideoConversionOptions options) {
        return CompletableFuture.supplyAsync(() -> convertVideo(inputPath, outputDir, options));
    }

    /**
     * Extracts the audio track from a video file.
     *
     * @param videoPath Path to the video file.
     * @param outputDir Directory for the output file.
     * @param options   Audio compression options for the output file.
     * @return The full path to the extracted audio file.
     * @throws MediaProcessingException if audio extraction fails.
     */
    public String extractAudio(String videoPath, Path outputDir, AudioCompressionOptions options) {
        Objects.requireNonNull(videoPath, "videoPath must not be null");
        Objects.requireNonNull(outputDir, "outputDir must not be null");
        Objects.requireNonNull(options, "options must not be null");

        String audioFilename = "audio_" + UUID.randomUUID() + "." + options.getFormat();
        Path audioPath = outputDir.resolve(audioFilename);

        try {
            // FIX: Replaced hardcoded values with flexible options from AudioCompressionOptions.
            FFmpeg.atPath(ffmpegPath)
                    .addInput(UrlInput.fromPath(Paths.get(videoPath)))
                    .addOutput(UrlOutput.toPath(audioPath)
                            .disableVideo()
                            .setAudioCodec(options.getAudioCodec())
                            .setAudioBitRate(options.getBitrate())
                            .setAudioSampleRate(options.getSampleRate())
                            .setOverwriteOutput(true))
                    .execute();

            log.info("Audio extraction completed: {}", audioPath);
            return audioPath.toString();

        } catch (Exception e) {
            log.error("Failed to extract audio from video: {}", videoPath, e);
            throw new MediaProcessingException("Audio extraction failed for: " + videoPath, e);
        }
    }

    /**
     * Compresses an audio file.
     *
     * @param inputPath Path to the input audio file.
     * @param outputDir Directory for the output file.
     * @param options   Compression options.
     * @return The full path to the compressed audio file.
     * @throws MediaProcessingException if compression fails.
     */
    public String compressAudio(String inputPath, Path outputDir, AudioCompressionOptions options) {
        Objects.requireNonNull(inputPath, "inputPath must not be null");
        Objects.requireNonNull(outputDir, "outputDir must not be null");
        Objects.requireNonNull(options, "options must not be null");

        String outputFilename = "compressed_" + UUID.randomUUID() + "." + options.getFormat();
        Path outputPath = outputDir.resolve(outputFilename);

        try {
            UrlOutput output = UrlOutput.toPath(outputPath)
                    .setOverwriteOutput(true)
                    .setFormat(options.getFormat());

            if (options.getBitrate() != null) {
                output.setAudioBitRate(options.getBitrate());
            }
            if (options.getSampleRate() != null) {
                output.setAudioSampleRate(options.getSampleRate());
            }
            if (options.getAudioCodec() != null) {
                // FIX: Use the new generic setCodec method
                output.setCodec(StreamType.AUDIO, options.getAudioCodec());
            }

            FFmpeg.atPath(ffmpegPath)
                    .addInput(UrlInput.fromPath(Paths.get(inputPath)))
                    .addOutput(output)
                    .execute();

            log.info("Audio compression completed: {}", outputPath);
            return outputPath.toString();

        } catch (Exception e) {
            log.error("Failed to compress audio: {}", inputPath, e);
            throw new MediaProcessingException("Audio compression failed for: " + inputPath, e);
        }
    }

    /**
     * Gets the duration of a video file in seconds.
     *
     * @param mediaPath Path to the media file.
     * @return An Optional containing the duration, or empty if it fails.
     */
    public Optional<Float> getMediaDuration(String mediaPath) {
        Objects.requireNonNull(mediaPath, "mediaPath must not be null");
        try {
            FFprobeResult result = getFfprobeResult(mediaPath);
            // FIX: Return Optional to better handle the failure case than returning 0.0.
            return Optional.ofNullable(result.getFormat().getDuration());
        } catch (Exception e) {
            log.error("Failed to get media duration: {}", mediaPath, e);
            return Optional.empty();
        }
    }

    /**
     * Checks if a file is a valid and readable media file.
     *
     * @param filePath Path to the file.
     * @return true if the file contains at least one media stream, false otherwise.
     */
    public boolean isValidMediaFile(String filePath) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        try {
            FFprobeResult result = getFfprobeResult(filePath);
            return result != null && result.getStreams() != null && !result.getStreams().isEmpty();
        } catch (Exception e) {
            log.warn("Could not probe file, it is likely not a valid media file: {}", filePath);
            return false;
        }
    }

    // --- Helper Methods ---

    /**
     * Parses the frame rate string (e.g., "30/1") into a double.
     */
    private double parseFrameRate(String frameRateString) {
        if (frameRateString == null || frameRateString.isEmpty()) {
            return 0.0;
        }
        String[] parts = frameRateString.split("/");
        if (parts.length == 2) {
            try {
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {
                log.warn("Could not parse frame rate: {}", frameRateString);
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * Executes ffprobe to get media information.
     */
    private FFprobeResult getFfprobeResult(String mediaPath) {
        return FFprobe.atPath(ffprobePath)
                .setInput(mediaPath)
                .setShowStreams(true)
                .setShowFormat(true)
                .execute();
    }
}
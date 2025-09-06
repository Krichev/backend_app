package com.my.challenger.service;

import com.github.kokorin.jaffree.ffmpeg.*;
import com.github.kokorin.jaffree.ffprobe.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MediaProcessingService {

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.ffprobe.path:ffprobe}")
    private String ffprobePath;

    public VideoMetadata extractVideoMetadata(String videoPath) {
        try {
            FFprobe ffprobe = FFprobe.atPath(Paths.get(ffprobePath));

            FFprobeResult result = ffprobe
                    .setInput(videoPath)
                    .setShowStreams(true)
                    .setShowFormat(true)
                    .execute();

            // Find video stream
            Stream videoStream = result.getStreams().stream()
                    .filter(stream -> StreamType.VIDEO.equals(stream.getCodecType()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No video stream found"));

            Format format = result.getFormat();

            VideoMetadata metadata = new VideoMetadata();
            metadata.setWidth(videoStream.getWidth());
            metadata.setHeight(videoStream.getHeight());
            metadata.setDurationSeconds((int) format.getDuration());
            metadata.setBitrate(format.getBitRate() != null ? format.getBitRate().intValue() : 0);

            // Parse frame rate
            if (videoStream.getRFrameRate() != null) {
                String[] parts = videoStream.getRFrameRate().split("/");
                if (parts.length == 2) {
                    double frameRate = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                    metadata.setFrameRate(frameRate);
                }
            }

            return metadata;

        } catch (Exception e) {
            log.error("Failed to extract video metadata from: {}", videoPath, e);
            throw new RuntimeException("Video metadata extraction failed", e);
        }
    }

    public AudioMetadata extractAudioMetadata(String audioPath) {
        try {
            FFprobe ffprobe = FFprobe.atPath(Paths.get(ffprobePath));

            FFprobeResult result = ffprobe
                    .setInput(audioPath)
                    .setShowStreams(true)
                    .setShowFormat(true)
                    .execute();

            // Find audio stream
            Stream audioStream = result.getStreams().stream()
                    .filter(stream -> StreamType.AUDIO.equals(stream.getCodecType()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No audio stream found"));

            Format format = result.getFormat();

            AudioMetadata metadata = new AudioMetadata();
            metadata.setDurationSeconds((int) format.getDuration());
            metadata.setBitrate(audioStream.getBitRate() != null ? audioStream.getBitRate().intValue() : 0);
            metadata.setSampleRate(audioStream.getSampleRate());
            metadata.setChannels(audioStream.getChannels());

            return metadata;

        } catch (Exception e) {
            log.error("Failed to extract audio metadata from: {}", audioPath, e);
            throw new RuntimeException("Audio metadata extraction failed", e);
        }
    }

    public String generateVideoThumbnail(String videoPath, Path outputDir) {
        try {
            String thumbnailFilename = "thumb_" + System.currentTimeMillis() + ".jpg";
            Path thumbnailPath = outputDir.resolve(thumbnailFilename);

            FFmpeg ffmpeg = FFmpeg.atPath(Paths.get(ffmpegPath));

            FFmpegResult result = ffmpeg
                    .addInput(UrlInput.fromPath(Paths.get(videoPath)))
                    .addOutput(UrlOutput.toPath(thumbnailPath)
                            .setVideoFilter("thumbnail,scale=320:240")
                            .setFrameCount(StreamType.VIDEO, 1L)
                            .setOverwriteOutput(true))
                    .setProgressListener(new ProgressListener() {
                        @Override
                        public void onProgress(FFmpegProgress progress) {
                            log.debug("Thumbnail generation progress: {}%",
                                    (progress.getTimeMillis() / 1000.0) * 100 / 30); // Assume 30s max
                        }
                    })
                    .execute();

            if (result.getVideoSize() > 0) {
                log.info("Successfully generated thumbnail: {}", thumbnailPath);
                return thumbnailPath.toString();
            } else {
                throw new RuntimeException("Thumbnail generation produced empty file");
            }

        } catch (Exception e) {
            log.error("Failed to generate video thumbnail from: {}", videoPath, e);
            throw new RuntimeException("Thumbnail generation failed", e);
        }
    }

    @Async
    public CompletableFuture<String> generateVideoThumbnailAsync(String videoPath, Path outputDir) {
        return CompletableFuture.supplyAsync(() -> generateVideoThumbnail(videoPath, outputDir));
    }

    /**
     * Convert video to different format/quality
     */
    public String convertVideo(String inputPath, Path outputDir, VideoConversionOptions options) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputFilename = "converted_" + timestamp + "." + options.getFormat();
            Path outputPath = outputDir.resolve(outputFilename);

            FFmpeg ffmpeg = FFmpeg.atPath(Paths.get(ffmpegPath));

            UrlOutput output = UrlOutput.toPath(outputPath)
                    .setOverwriteOutput(true);

            // Apply video options
            if (options.getWidth() != null && options.getHeight() != null) {
                output.setVideoFilter("scale=" + options.getWidth() + ":" + options.getHeight());
            }

            if (options.getBitrate() != null) {
                output.setVideoBitRate(options.getBitrate());
            }

            if (options.getFrameRate() != null) {
                output.setFrameRate(options.getFrameRate());
            }

            FFmpegResult result = ffmpeg
                    .addInput(UrlInput.fromPath(Paths.get(inputPath)))
                    .addOutput(output)
                    .setProgressListener(progress -> {
                        log.debug("Video conversion progress: {} frames", progress.getFrame());
                    })
                    .execute();

            log.info("Video conversion completed: {} -> {}", inputPath, outputPath);
            return outputPath.toString();

        } catch (Exception e) {
            log.error("Failed to convert video: {}", inputPath, e);
            throw new RuntimeException("Video conversion failed", e);
        }
    }

    @Async
    public CompletableFuture<String> convertVideoAsync(String inputPath, Path outputDir, VideoConversionOptions options) {
        return CompletableFuture.supplyAsync(() -> convertVideo(inputPath, outputDir, options));
    }

    /**
     * Extract audio from video
     */
    public String extractAudio(String videoPath, Path outputDir, String format) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String audioFilename = "audio_" + timestamp + "." + format;
            Path audioPath = outputDir.resolve(audioFilename);

            FFmpeg ffmpeg = FFmpeg.atPath(Paths.get(ffmpegPath));

            FFmpegResult result = ffmpeg
                    .addInput(UrlInput.fromPath(Paths.get(videoPath)))
                    .addOutput(UrlOutput.toPath(audioPath)
                            .disableVideo()
                            .setAudioCodec("aac") // or mp3, depending on format
                            .setOverwriteOutput(true))
                    .execute();

            log.info("Audio extraction completed: {}", audioPath);
            return audioPath.toString();

        } catch (Exception e) {
            log.error("Failed to extract audio from video: {}", videoPath, e);
            throw new RuntimeException("Audio extraction failed", e);
        }
    }

    /**
     * Compress audio file
     */
    public String compressAudio(String inputPath, Path outputDir, AudioCompressionOptions options) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String outputFilename = "compressed_" + timestamp + "." + options.getFormat();
            Path outputPath = outputDir.resolve(outputFilename);

            FFmpeg ffmpeg = FFmpeg.atPath(Paths.get(ffmpegPath));

            UrlOutput output = UrlOutput.toPath(outputPath)
                    .setOverwriteOutput(true);

            if (options.getBitrate() != null) {
                output.setAudioBitRate(options.getBitrate());
            }

            if (options.getSampleRate() != null) {
                output.setAudioSampleRate(options.getSampleRate());
            }

            FFmpegResult result = ffmpeg
                    .addInput(UrlInput.fromPath(Paths.get(inputPath)))
                    .addOutput(output)
                    .execute();

            log.info("Audio compression completed: {}", outputPath);
            return outputPath.toString();

        } catch (Exception e) {
            log.error("Failed to compress audio: {}", inputPath, e);
            throw new RuntimeException("Audio compression failed", e);
        }
    }

    /**
     * Get video duration quickly (without full metadata extraction)
     */
    public double getVideoDuration(String videoPath) {
        try {
            FFprobe ffprobe = FFprobe.atPath(Paths.get(ffprobePath));

            FFprobeResult result = ffprobe
                    .setInput(videoPath)
                    .setShowFormat(true)
                    .execute();

            return result.getFormat().getDuration();

        } catch (Exception e) {
            log.error("Failed to get video duration: {}", videoPath, e);
            return 0.0;
        }
    }

    /**
     * Check if file is valid media
     */
    public boolean isValidMediaFile(String filePath) {
        try {
            FFprobe ffprobe = FFprobe.atPath(Paths.get(ffprobePath));

            FFprobeResult result = ffprobe
                    .setInput(filePath)
                    .setShowStreams(true)
                    .execute();

            return !result.getStreams().isEmpty();

        } catch (Exception e) {
            log.debug("File is not valid media: {}", filePath);
            return false;
        }
    }
}

@Data
class VideoMetadata {
    private int width;
    private int height;
    private int durationSeconds;
    private int bitrate;
    private double frameRate;
    private String codec;
    private String pixelFormat;
}

@Data
class AudioMetadata {
    private int durationSeconds;
    private int bitrate;
    private int sampleRate;
    private int channels;
    private String codec;
}

@Data
class VideoConversionOptions {
    private String format = "mp4";
    private Integer width;
    private Integer height;
    private Integer bitrate;
    private Double frameRate;
    private String codec = "h264";
    private String quality = "medium"; // fast, medium, slow
}

@Data
class AudioCompressionOptions {
    private String format = "mp3";
    private Integer bitrate = 128; // kbps
    private Integer sampleRate = 44100; // Hz
    private String codec = "mp3";
}
package com.my.challenger.service.impl;

import com.github.kokorin.jaffree.Rational;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MediaProcessingService {

    private final Path ffmpegPath;
    private final Path ffprobePath;

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

            return VideoMetadata.builder()
                    .width(videoStream.getWidth())
                    .height(videoStream.getHeight())
                    .durationSeconds(format.getDuration())
                    .bitrate(format.getBitRate() != null ? format.getBitRate() : 0L)
                    .frameRate(parseFrameRate(videoStream.getRFrameRate()))
                    .codec(videoStream.getCodecName())
                    .pixelFormat(videoStream.getPixFmt())   // ✅ FIXED
                    .build();


        } catch (Exception e) {
            log.error("Failed to extract video metadata from: {}", videoPath, e);
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

            return AudioMetadata.builder()
                    .durationSeconds(format.getDuration())
                    .bitrate(audioStream.getBitRate() != null ? audioStream.getBitRate() :
                            (format.getBitRate() != null ? format.getBitRate() : 0L))
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
     * @param videoPath Path to the video file.
     * @param outputDir Directory to save the thumbnail in.
     * @param seekTime  The time from which to grab the thumbnail.
     * @return The full path to the generated thumbnail.
     * @throws MediaProcessingException if thumbnail generation fails.
     */
    public String generateVideoThumbnail(String videoPath, Path outputDir, Duration seekTime) {
        Objects.requireNonNull(videoPath, "videoPath must not be null");
        Objects.requireNonNull(outputDir, "outputDir must not be null");
        Objects.requireNonNull(seekTime, "seekTime must not be null");

        String thumbnailFilename = "thumb_" + UUID.randomUUID() + ".jpg";
        Path thumbnailPath = outputDir.resolve(thumbnailFilename);

        try {
            FFmpeg.atPath(ffmpegPath)
                    .addInput(UrlInput.fromPath(Paths.get(videoPath))
                            .setPosition(seekTime.toMillis(), TimeUnit.MILLISECONDS))
                    .addOutput(UrlOutput.toPath(thumbnailPath)
                            .setFrameCount(StreamType.VIDEO, 1L)  // Extract only 1 frame
                            .disableStream(StreamType.AUDIO)       // No audio in thumbnail
                            .addArgument("-y"))
                    .setProgressListener(progress ->
                            log.debug("Thumbnail generation progress: {}ms", progress.getTimeMillis()))
                    .execute();

            log.info("Successfully generated thumbnail: {}", thumbnailPath);
            return thumbnailPath.toString();

        } catch (Exception e) {
            log.error("Failed to generate video thumbnail from: {}", videoPath, e);
            throw new MediaProcessingException("Thumbnail generation failed for: " + videoPath, e);
        }
    }

    /**
     * Asynchronously generates a single thumbnail from a video.
     *
     * @param videoPath Path to the video file.
     * @param outputDir Directory to save the thumbnail in.
     * @return A CompletableFuture containing the path to the thumbnail.
     */
    @Async
    public CompletableFuture<String> generateVideoThumbnailAsync(String videoPath, Path outputDir) {
        return CompletableFuture.supplyAsync(() ->
                generateVideoThumbnail(videoPath, outputDir, Duration.ofSeconds(5)));
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
            UrlOutput output = UrlOutput.toPath(outputPath)
                    .addArgument("-y")
                    .setFormat(options.getFormat());

            // Apply video options
            if (options.getWidth() != null && options.getHeight() != null) {
                output.addArguments("-vf", "scale=" + options.getWidth() + ":" + options.getHeight());
            }
            if (options.getBitrate() != null) {
                output.addArguments("-b:v", options.getBitrate() + "k");
            }
            if (options.getFrameRate() != null) {
                output.setFrameRate(options.getFrameRate());
            }
            if (options.getVideoCodec() != null) {
                output.setCodec(StreamType.VIDEO, options.getVideoCodec());
            }
            if (options.getAudioCodec() != null) {
                output.setCodec(StreamType.AUDIO, options.getAudioCodec());
            }

            // Add quality preset
            String preset = options.getQualityPreset() != null ? options.getQualityPreset() : "medium";

            FFmpeg ffmpeg = FFmpeg.atPath(ffmpegPath)
                    .addInput(UrlInput.fromPath(Paths.get(inputPath)))
                    .addOutput(output)
                    .addArguments("-preset", preset)
                    .setProgressListener(progress ->
                            log.debug("Video conversion progress: frame {}", progress.getFrame()));

            ffmpeg.execute();

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
    public String extractAudioFromVideo(String videoPath, Path outputDir, AudioCompressionOptions options) {
        Objects.requireNonNull(videoPath, "videoPath must not be null");
        Objects.requireNonNull(outputDir, "outputDir must not be null");
        Objects.requireNonNull(options, "options must not be null");

        String format = options.getFormat() != null ? options.getFormat() : "mp3";
        String outputFilename = "audio_" + UUID.randomUUID() + "." + format;
        Path outputPath = outputDir.resolve(outputFilename);

        try {
            UrlOutput output = UrlOutput.toPath(outputPath)
                    .disableStream(StreamType.VIDEO)  // No video stream
                    .addArgument("-y");

            // Apply audio options
            if (options.getAudioCodec() != null) {
                output.setCodec(StreamType.AUDIO, options.getAudioCodec());
            }
            if (options.getBitrate() != null) {
                output.addArguments("-b:a", options.getBitrate() + "k");
            }
            if (options.getSampleRate() != null) {
                output.addArguments("-ar", String.valueOf(options.getSampleRate()));
            }
            if (options.getChannels() != null) {
                output.addArguments("-ac", String.valueOf(options.getChannels()));
            }

            FFmpeg.atPath(ffmpegPath)
                    .addInput(UrlInput.fromPath(Paths.get(videoPath)))
                    .addOutput(output)
                    .setProgressListener(progress ->
                            log.debug("Audio extraction progress: {}ms", progress.getTimeMillis()))
                    .execute();

            log.info("Audio extraction completed: {} -> {}", videoPath, outputPath);
            return outputPath.toString();

        } catch (Exception e) {
            log.error("Failed to extract audio from video: {}", videoPath, e);
            throw new MediaProcessingException("Audio extraction failed for: " + videoPath, e);
        }
    }


    private double parseFrameRate(Rational rational) {
        if (rational == null) return 0.0;
        return rational.doubleValue();   // ✅ Rational has doubleValue()
    }

    /**
     * Compresses an audio file according to the specified options.
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

        String format = options.getFormat() != null ? options.getFormat() : "mp3";
        String outputFilename = "compressed_" + UUID.randomUUID() + "." + format;
        Path outputPath = outputDir.resolve(outputFilename);

        try {
            UrlOutput output = UrlOutput.toPath(outputPath)
                    .addArgument("-y");

            // Apply audio compression options
            if (options.getAudioCodec() != null) {
                output.setCodec(StreamType.AUDIO, options.getAudioCodec());
            }
            if (options.getBitrate() != null) {
                output.addArguments("-b:a", options.getBitrate() + "k");
            }
            if (options.getSampleRate() != null) {
                output.addArguments("-ar", String.valueOf(options.getSampleRate()));
            }
            if (options.getChannels() != null) {
                output.addArguments("-ac", String.valueOf(options.getChannels()));
            }

            FFmpeg.atPath(ffmpegPath)
                    .addInput(UrlInput.fromPath(Paths.get(inputPath)))
                    .addOutput(output)
                    .setProgressListener(progress ->
                            log.debug("Audio compression progress: {}ms", progress.getTimeMillis()))
                    .execute();

            log.info("Audio compression completed: {} -> {}", inputPath, outputPath);
            return outputPath.toString();

        } catch (Exception e) {
            log.error("Failed to compress audio: {}", inputPath, e);
            throw new MediaProcessingException("Audio compression failed for: " + inputPath, e);
        }
    }

    /**
     * Merges multiple video files into one.
     *
     * @param videoPaths List of video file paths to merge.
     * @param outputDir  Directory for the output file.
     * @param format     Output format (e.g., "mp4").
     * @return The full path to the merged video file.
     * @throws MediaProcessingException if merging fails.
     */
    public String mergeVideos(java.util.List<String> videoPaths, Path outputDir, String format) {
        if (videoPaths == null || videoPaths.isEmpty()) {
            throw new IllegalArgumentException("Video paths list cannot be null or empty");
        }
        Objects.requireNonNull(outputDir, "outputDir must not be null");

        String outputFormat = format != null ? format : "mp4";
        String outputFilename = "merged_" + UUID.randomUUID() + "." + outputFormat;
        Path outputPath = outputDir.resolve(outputFilename);

        try {
            // Create concat demuxer input file
            Path concatFile = outputDir.resolve("concat_" + UUID.randomUUID() + ".txt");
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (String videoPath : videoPaths) {
                lines.add("file '" + videoPath + "'");
            }
            java.nio.file.Files.write(concatFile, lines);

            FFmpeg.atPath(ffmpegPath)
                    .addArguments("-f", "concat")
                    .addArguments("-safe", "0")
                    .addInput(UrlInput.fromPath(concatFile))
                    .addArguments("-c", "copy")
                    .addOutput(UrlOutput.toPath(outputPath)
                            .addArgument("-y"))
                    .setProgressListener(progress ->
                            log.debug("Video merge progress: frame {}", progress.getFrame()))
                    .execute();

            // Clean up temp file
            java.nio.file.Files.deleteIfExists(concatFile);

            log.info("Video merge completed: {} videos -> {}", videoPaths.size(), outputPath);
            return outputPath.toString();

        } catch (Exception e) {
            log.error("Failed to merge videos", e);
            throw new MediaProcessingException("Video merge failed", e);
        }
    }

    /**
     * Helper method to get FFprobe result.
     */
    private FFprobeResult getFfprobeResult(String filePath) {
        return FFprobe.atPath(ffprobePath)
                .setShowStreams(true)
                .setShowFormat(true)
                .setInput(Paths.get(filePath))
                .execute();
    }

    /**
     * Parses frame rate from FFprobe string format (e.g., "30/1" -> 30.0).
     */
    private double parseFrameRate(String frameRateStr) {
        if (frameRateStr == null || frameRateStr.isEmpty()) {
            return 0.0;
        }

        try {
            if (frameRateStr.contains("/")) {
                String[] parts = frameRateStr.split("/");
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            } else {
                return Double.parseDouble(frameRateStr);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse frame rate: {}", frameRateStr);
            return 0.0;
        }
    }
}
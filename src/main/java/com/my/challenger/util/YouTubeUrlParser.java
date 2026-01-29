package com.my.challenger.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeUrlParser {

    private static final String YOUTUBE_VIDEO_ID_REGEX = 
            "(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:[^\\/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?)\\/|\\S*?[?&]v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})";
    
    private static final Pattern PATTERN = Pattern.compile(YOUTUBE_VIDEO_ID_REGEX, Pattern.CASE_INSENSITIVE);

    public static Optional<String> extractVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return Optional.empty();
        }
        
        Matcher matcher = PATTERN.matcher(url);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public static boolean isYouTubeUrl(String url) {
        return extractVideoId(url).isPresent();
    }

    public static String buildEmbedUrl(String videoId, Double startTime, Double endTime) {
        if (videoId == null) return null;
        
        StringBuilder sb = new StringBuilder("https://www.youtube.com/embed/");
        sb.append(videoId);
        sb.append("?autoplay=0&rel=0");
        
        if (startTime != null) {
            sb.append("&start=").append(startTime.intValue());
        }
        
        if (endTime != null) {
            sb.append("&end=").append(endTime.intValue());
        }
        
        return sb.toString();
    }
}

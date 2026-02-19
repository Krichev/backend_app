package com.my.challenger.dto.appversion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubReleaseDTO {
    @JsonProperty("tag_name")
    private String tagName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("body")
    private String body;

    @JsonProperty("published_at")
    private String publishedAt;

    @JsonProperty("assets")
    private List<GitHubAsset> assets;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitHubAsset {
        @JsonProperty("browser_download_url")
        private String browserDownloadUrl;

        @JsonProperty("name")
        private String name;

        @JsonProperty("size")
        private Long size;
    }
}

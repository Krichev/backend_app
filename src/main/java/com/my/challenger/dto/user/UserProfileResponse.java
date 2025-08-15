package com.my.challenger.dto.user;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User profile information")
public class UserProfileResponse {

    @Schema(description = "User ID", example = "1")
    private String id;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Schema(description = "User bio", example = "I love challenges!")
    private String bio;

    @Schema(description = "Avatar URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "Account creation date", example = "2024-01-15T10:30:00")
    private String createdAt;

    @Schema(description = "Number of completed challenges")
    private Integer statsCompleted;

    @Schema(description = "Number of created challenges")
    private Integer statsCreated;

    @Schema(description = "Success rate percentage")
    private Double statsSuccess;
}
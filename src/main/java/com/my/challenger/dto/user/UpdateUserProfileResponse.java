// src/main/java/com/my/challenger/dto/user/UpdateUserProfileResponse.java
package com.my.challenger.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after updating user profile")
public class UpdateUserProfileResponse {

    @Schema(description = "Updated user profile", required = true)
    private UserProfileResponse user;

    @Schema(description = "New JWT token (provided when username is changed)", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String newToken;

    @Schema(description = "Message indicating what was updated")
    private String message;

    // Helper constructors
    public UpdateUserProfileResponse(UserProfileResponse user) {
        this.user = user;
        this.message = "Profile updated successfully";
    }

    public UpdateUserProfileResponse(UserProfileResponse user, String newToken) {
        this.user = user;
        this.newToken = newToken;
        this.message = "Profile updated successfully. New authentication token provided due to username change.";
    }
}
// UpdateUserProfileRequest.java
package com.my.challenger.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update user profile")
public class UpdateUserProfileRequest {

    @Schema(description = "Username", example = "john_doe", maxLength = 50)
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(description = "User bio", example = "I love challenges!", maxLength = 500)
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    @Schema(description = "Avatar URL", example = "https://example.com/avatar.jpg")
    private String avatar;
}



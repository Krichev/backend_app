
// src/main/java/com/my/challenger/dto/auth/RefreshTokenResponse.java
package com.my.challenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {
    private String accessToken;
    private String refreshToken;
    private UserDTO user;
    private String tokenType = "Bearer";
}
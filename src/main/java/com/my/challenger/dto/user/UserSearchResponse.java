package com.my.challenger.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private String id;
    private String username;
    private String avatar;
    private String bio;
    private String connectionStatus; // NONE, PENDING, ACCEPTED
    private int mutualConnectionsCount;
}

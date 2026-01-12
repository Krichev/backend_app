package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResultDTO {
    private Long id;
    private String username;
    private String avatar;
    private String bio;
    private long mutualConnectionsCount;
    private String connectionStatus; // NONE, PENDING_SENT, PENDING_RECEIVED, CONNECTED
}

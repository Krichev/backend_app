package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSuggestionDTO {
    private Long id;
    private String username;
    private String avatar;
    private long mutualConnectionsCount;
}

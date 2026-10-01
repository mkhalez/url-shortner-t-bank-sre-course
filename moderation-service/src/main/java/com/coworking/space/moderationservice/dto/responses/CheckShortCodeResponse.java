package com.coworking.space.moderationservice.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
public class CheckShortCodeResponse {
    private String shortCode;

    private ShortCodeCheckStatus status;
}

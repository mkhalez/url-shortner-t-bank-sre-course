package com.coworking.space.urlservice.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class CheckShortCodeResponse {
    private String shortCode;

    private ShortCodeCheckStatus status;
}

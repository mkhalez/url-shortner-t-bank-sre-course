package com.coworking.space.urlservice.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckShortCodeRequest {
    private String shortCode;
}

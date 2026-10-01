package com.coworking.space.urlservice.dto.requests;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CreateRandomUrlRequest {
    private String longUrl;
}

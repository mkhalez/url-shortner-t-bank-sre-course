package com.coworking.space.urlservice.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CreateCustomUrlRequest {
    @NotBlank
    private String longUrl;

    private String customShortCode;
}

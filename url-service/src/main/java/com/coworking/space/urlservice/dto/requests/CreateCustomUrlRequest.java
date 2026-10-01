package com.coworking.space.urlservice.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@NoArgsConstructor
@Getter
public class CreateCustomUrlRequest {
    @NotBlank(message = "long url must be not blank")
    @URL(message = "long url must be url")
    private String longUrl;

    @NotBlank(message = "custom short code must be not blank")
    @Pattern(regexp = "[a-zA-Z0-9_-]{5,10}", message = "custom short code must match [a-zA-Z0-9_-]{5,10}")
    private String customShortCode;
}

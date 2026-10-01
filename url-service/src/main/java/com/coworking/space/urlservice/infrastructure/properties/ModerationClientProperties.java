package com.coworking.space.urlservice.infrastructure.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "moderation-client")
@AllArgsConstructor
@Validated
@Getter
public class ModerationClientProperties {
    @NotBlank
    private String baseUrl;

    @Positive
    private int connectionTimeout;

    @Positive
    private int readTimeout;
}

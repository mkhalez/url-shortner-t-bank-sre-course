package com.coworking.space.urlservice.infrastructure.properties;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "url")
@AllArgsConstructor
@Getter
@Validated
public class UrlServiceProperties {
    @Positive
    private int shortUrlLength;
}

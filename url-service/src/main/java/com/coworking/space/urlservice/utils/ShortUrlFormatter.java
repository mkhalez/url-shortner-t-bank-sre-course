package com.coworking.space.urlservice.utils;

import com.coworking.space.urlservice.infrastructure.properties.UrlProperties;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShortUrlFormatter {
    private final UrlProperties urlProperties;

    @Named("toFullUrl")
    public String toFullUrl(String shortCode) {
        return urlProperties.getHost() + shortCode;
    }
}

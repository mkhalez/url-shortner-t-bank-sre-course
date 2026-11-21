package com.coworking.space.urlservice.controllers;

import com.coworking.space.urlservice.dto.responses.VersionResponse;
import com.coworking.space.urlservice.infrastructure.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VersionController {
    private final AppProperties appProperties;

    @GetMapping("/version")
    public VersionResponse version() {
        return new VersionResponse(appProperties.version(), appProperties.podName());
    }
}
